package ca.bc.gov.educ.penreg.api.messaging;

import io.nats.client.Connection;
import io.nats.client.Message;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The type Message publisher.
 */
@Component
@Slf4j
public class MessagePublisher {


  private final Connection connection;

  @Autowired
  public MessagePublisher(final Connection con) {
    this.connection = con;
  }

  /**
   * Dispatch message.
   *
   * @param subject the subject
   * @param message the message
   */
  public void dispatchMessage(final String subject, final byte[] message) {
    this.connection.publish(subject, message);
  }

  public CompletableFuture<Message> requestMessage(final String subject, final byte[] message) {
    return this.connection.request(subject, message);
  }
}
