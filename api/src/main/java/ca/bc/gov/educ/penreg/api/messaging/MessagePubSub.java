package ca.bc.gov.educ.penreg.api.messaging;

import io.nats.client.Connection;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The type Message pub sub.
 */
@Slf4j
@SuppressWarnings("java:S2142")
public abstract class MessagePubSub implements Closeable {
  /**
   * The Connection.
   */
  protected Connection connection;
  /**
   * The Executor service.
   */
  @Setter
  protected ExecutorService executorService = Executors.newSingleThreadExecutor();

  /**
   * Close.
   */
  @Override
  public void close() {
    if (!executorService.isShutdown()) {
      executorService.shutdown();
    }
    if(Optional.ofNullable(connection).isPresent()){
      log.info("closing nats connection...");
      try {
        connection.close();
      } catch (InterruptedException e) {
        log.error("error while closing nats connection ...", e);
      }
      log.info("nats connection closed...");
    }
  }

}
