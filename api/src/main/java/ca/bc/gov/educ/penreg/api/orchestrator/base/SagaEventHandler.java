package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.struct.Event;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * The interface Saga event handler.
 */
public interface SagaEventHandler {
  /**
   * On saga event.
   *
   * @param event the event
   * @throws InterruptedException the interrupted exception
   * @throws IOException          the io exception
   * @throws TimeoutException     the timeout exception
   */
  void onSagaEvent(Event event) throws InterruptedException, IOException, TimeoutException;
}