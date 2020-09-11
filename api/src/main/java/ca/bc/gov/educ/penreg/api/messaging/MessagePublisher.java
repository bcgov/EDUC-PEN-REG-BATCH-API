package ca.bc.gov.educ.penreg.api.messaging;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import io.nats.streaming.AckHandler;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Message publisher.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class MessagePublisher extends MessagePubSub {


  @Autowired
  public MessagePublisher(final ApplicationProperties applicationProperties) throws IOException, InterruptedException {
    this(applicationProperties, true);
  }

  /**
   * Instantiates a new Message publisher.
   *
   * @param applicationProperties the application properties
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   */
  public MessagePublisher(final ApplicationProperties applicationProperties, boolean isConnectionRequired) throws IOException, InterruptedException {
    Options.Builder builder = new Options.Builder();
    builder.natsUrl(applicationProperties.getNatsUrl());
    builder.clusterId(applicationProperties.getNatsClusterId());
    builder.clientId("pen-reg-batch-api-publisher-" + UUID.randomUUID().toString());
    builder.connectionLostHandler(this::connectionLostHandler);
    Options options = builder.build();
    connectionFactory = new StreamingConnectionFactory(options);
    if (isConnectionRequired) {
      this.connect();
    }

  }

  protected void connect() throws IOException, InterruptedException {
    this.connection = connectionFactory.createConnection();
  }


  /**
   * Dispatch message.
   *
   * @param subject the subject
   * @param message the message
   */
  public void dispatchMessage(String subject, byte[] message) {
    try {
      connection.publish(subject, message, getAckHandler());
    } catch (IOException | InterruptedException | TimeoutException e) {
      executorService.execute(() -> retryPublish(subject, message));
    }
  }

  /**
   * This is a blocking call which makes sure that the message was delivered to the other side.
   * Send message.
   *
   * @param subject the subject
   * @param message the message
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  public void sendMessage(String subject, byte[] message) throws InterruptedException, TimeoutException, IOException {
    connection.publish(subject, message);
  }


  /**
   * Gets ack handler.
   *
   * @return the ack handler
   */
  protected AckHandler getAckHandler() {
    return new AckHandler() {
      @Override
      public void onAck(String guid, Exception err) {
        log.trace("already handled.");
      }

      @Override
      public void onAck(String guid, String subject, byte[] data, Exception ex) {
        if (ex != null) {
          executorService.execute(() -> retryPublish(subject, data));
        } else {
          log.trace("acknowledgement received {}", guid);
        }
      }
    };
  }

  /**
   * Retry publish.
   *
   * @param subject the subject
   * @param message the message
   */
  public void retryPublish(String subject, byte[] message) {
    log.trace("retrying...");
    while (true) {
      try {
        connection.publish(subject, message, getAckHandler());
        break;
      } catch (IOException | InterruptedException | TimeoutException e) {
        log.error("Exception while trying to publish");
      }
    }

  }


}
