package ca.bc.gov.educ.penreg.api.messaging;

import ca.bc.gov.educ.penreg.api.helpers.LogHelper;
import ca.bc.gov.educ.penreg.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class MessageSubscriber {

  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, EventHandler> handlerMap = new HashMap<>();
  private final Connection connection;

  @Autowired
  public MessageSubscriber(final Connection con, final List<EventHandler> eventHandlers) {
    this.connection = con;
    eventHandlers.forEach(handler -> {
      this.handlerMap.put(handler.getTopicToSubscribe(), handler);
      this.subscribe(handler.getTopicToSubscribe(), handler);
    });
  }

  public void subscribe(final String topic, final EventHandler eventHandler) {
    this.handlerMap.computeIfAbsent(topic, k -> eventHandler);
    final String queue = topic.replace("_", "-");
    final var dispatcher = this.connection.createDispatcher(this.onMessage(eventHandler));
    dispatcher.subscribe(topic, queue);
  }

  /**
   * On message message handler.
   *
   * @return the message handler
   */
  public MessageHandler onMessage(final EventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received subject :: {},  replyTo :: {}, subscriptionID :: {}", message.getSubject(), message.getReplyTo(), message.getSID());
        try {
          final var eventString = new String(message.getData());
          LogHelper.logMessagingEventDetails(eventString);
          final var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.handleEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }
}
