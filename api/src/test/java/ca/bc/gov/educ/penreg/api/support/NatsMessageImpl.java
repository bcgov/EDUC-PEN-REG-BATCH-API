package ca.bc.gov.educ.penreg.api.support;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Support class to use for testing.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NatsMessageImpl implements Message {
  private String subject;
  private Subscription subscription;
  private String replyTo;
  private byte[] data;
  private String SID;
  private Connection connection;

  /**
   * @return true if there are headers
   */
  @Override
  public boolean hasHeaders() {
    return false;
  }

  /**
   * @return the headers object the message
   */
  @Override
  public Headers getHeaders() {
    return null;
  }

  /**
   * @return true if there is status
   */
  @Override
  public boolean isStatusMessage() {
    return false;
  }

  /**
   * @return the status object message
   */
  @Override
  public Status getStatus() {
    return null;
  }

  /**
   * @return if is utf8Mode
   */
  @Override
  public boolean isUtf8mode() {
    return false;
  }

  /**
   * Gets the metadata associated with a JetStream message.
   *
   * @return metadata or null if the message is not a JetStream message.
   */
  @Override
  public NatsJetStreamMetaData metaData() {
    return null;
  }

  /**
   * ack acknowledges a JetStream messages received from a Consumer, indicating the message
   * should not be received again later.
   */
  @Override
  public void ack() {

  }

  /**
   * ack acknowledges a JetStream messages received from a Consumer, indicating the message
   * should not be received again later.  Duration.ZERO does not confirm the acknowledgement.
   *
   * @param timeout the duration to wait for an ack confirmation
   * @throws TimeoutException     if a timeout was specified and the NATS server does not return a response
   * @throws InterruptedException if the thread is interrupted
   */
  @Override
  public void ackSync(final Duration timeout) throws TimeoutException, InterruptedException {

  }

  /**
   * nak acknowledges a JetStream message has been received but indicates that the message
   * is not completely processed and should be sent again later.
   */
  @Override
  public void nak() {

  }

  /**
   * term prevents this message from every being delivered regardless of maxDeliverCount.
   */
  @Override
  public void term() {

  }

  /**
   * Indicates that this message is being worked on and reset redelivery timer in the server.
   */
  @Override
  public void inProgress() {

  }

  /**
   * Checks if a message is from Jetstream or is a standard message.
   *
   * @return true if the message is from JetStream.
   */
  @Override
  public boolean isJetStream() {
    return false;
  }
}
