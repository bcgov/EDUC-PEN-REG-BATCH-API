package ca.bc.gov.educ.penreg.api.messaging;

import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.orchestrator.base.SagaEventHandler;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import io.nats.streaming.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

/**
 * This listener uses durable queue groups of nats streaming client. A durable
 * queue group allows you to have all members leave but still maintain state.
 * When a member re-joins, it starts at the last position in that group. <b>DO
 * NOT call unsubscribe on the subscription.</b> please see the below for
 * details. Closing the Group The last member calling Unsubscribe will close
 * (that is destroy) the group. So if you want to maintain durability of the
 * group, <b>you should not be calling Unsubscribe.</b>
 * <p>
 * So unlike for non-durable queue subscribers, it is possible to maintain a
 * queue group with no member in the server. When a new member re-joins the
 * durable queue group, it will resume from where the group left of, actually
 * first receiving all unacknowledged messages that may have been left when the
 * last member previously left.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class MessageSubscriber extends MessagePubSub {

  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, SagaEventHandler> handlers = new HashMap<>();

  /**
   * Instantiates a new Message subscriber.
   *
   * @param applicationProperties the application properties
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   */
  @Autowired
  public MessageSubscriber(final ApplicationProperties applicationProperties) throws IOException, InterruptedException {
    Options options = new Options.Builder().natsUrl(applicationProperties.getNatsUrl())
        .clusterId(applicationProperties.getNatsClusterId())
        .clientId("pen-reg-batch-api-subscriber-" + UUID.randomUUID().toString())
        .connectionLostHandler(this::connectionLostHandler).build();
    connectionFactory = new StreamingConnectionFactory(options);
    connection = connectionFactory.createConnection();
  }

  /**
   * This subscription will makes sure the messages are required to acknowledge manually to STAN.
   * Subscribe.
   *
   * @param topic        the topic
   * @param eventHandler the event handler
   */
  public void subscribe(String topic, SagaEventHandler eventHandler) {
    if(!handlers.containsKey(topic)){
      handlers.put(topic, eventHandler);
    }

    String queue = topic.replace("_", "-");
    SubscriptionOptions options = new SubscriptionOptions.Builder().manualAcks().ackWait(Duration.ofMinutes(2)).durableName(queue + "-consumer").build();// ":" is not allowed in durable name by NATS.
    try {
      connection.subscribe(topic, queue, onMessage(eventHandler), options);
    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new PenRegAPIRuntimeException(e.getMessage());
    }
  }

  /**
   * On message message handler.
   *
   * @param eventHandler the event handler
   * @return the message handler
   */
  private MessageHandler onMessage(SagaEventHandler eventHandler) {
    return (Message message) ->  {
      if (message != null) {
        log.trace("Message received is :: {} ", message);
        try {
          eventHandler.onSagaEvent(message);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }

  /**
   * This method will keep retrying for a connection.
   *
   * @param streamingConnection the streaming connection
   * @param e                   the e
   * @return the int
   */
  protected int connectionLostHandler(StreamingConnection streamingConnection, Exception e) {
    int numOfRetries = 1;
    if (e != null) {
      numOfRetries = super.connectionLostHandler(streamingConnection,e);
      retrySubscription(numOfRetries);
    }
    return numOfRetries;
  }

  /**
   * Retry subscription.
   *
   * @param numOfRetries the num of retries
   */
  private void retrySubscription(int numOfRetries) {
    while (true) {
      try {
        log.trace("retrying subscription as connection was lost :: retrying ::" + numOfRetries++);
        handlers.forEach(this::subscribe);
        log.info("successfully resubscribed after {} attempts", numOfRetries);
        break;
      } catch (PenRegAPIRuntimeException exception) {
        log.error("exception occurred while retrying subscription", exception);
        try {
          double sleepTime = (2 * numOfRetries);
          TimeUnit.SECONDS.sleep((long) sleepTime);
        } catch (InterruptedException exc) {
          log.error("InterruptedException occurred while retrying subscription", exc);
        }
      }
    }
  }
}
