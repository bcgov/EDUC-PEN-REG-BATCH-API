package ca.bc.gov.educ.penreg.api.orchestrator.base;

import io.nats.streaming.Message;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * The interface Saga event handler.
 */
public interface SagaEventHandler {
  /**
   * On saga event.
   *
   * @param message the message, this is passed to make manual acknowledgement to STAN.
   * @throws InterruptedException the interrupted exception
   * @throws IOException          the io exception
   * @throws TimeoutException     the timeout exception
   */
  void onSagaEvent(Message message) throws InterruptedException, IOException, TimeoutException;
}