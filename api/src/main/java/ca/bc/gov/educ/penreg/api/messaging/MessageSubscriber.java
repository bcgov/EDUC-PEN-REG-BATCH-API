package ca.bc.gov.educ.penreg.api.messaging;

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
public class MessageSubscriber extends MessagePubSub {

  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, EventHandler> handlerMap = new HashMap<>();

  @Autowired
  public MessageSubscriber(final Connection con, final List<EventHandler> eventHandlers) {
    super.connection = con;
    eventHandlers.forEach(handler -> {
      handlerMap.put(handler.getTopicToSubscribe(), handler);
      subscribe(handler.getTopicToSubscribe(), handler);
    });
  }

  /**
   * This subscription will makes sure the messages are required to acknowledge manually to STAN.
   * Subscribe.
   */
  public void subscribe(String topic, EventHandler eventHandler) {
    if (!handlerMap.containsKey(topic)) {
      handlerMap.put(topic, eventHandler);
    }
    String queue = topic.replace("_", "-");
    var dispatcher = connection.createDispatcher(onMessage(eventHandler));
    dispatcher.subscribe(topic, queue);
  }

  /**
   * On message message handler.
   *
   * @return the message handler
   */
  public MessageHandler onMessage(EventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received subject :: {},  replyTo :: {}, subscriptionID :: {}", message.getSubject(), message.getReplyTo(), message.getSID());
        try {
          var eventString = new String(message.getData());
          var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.handleEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }
}
