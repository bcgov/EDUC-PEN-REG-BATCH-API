package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.struct.Event;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * The interface event handler.
 */
public interface EventHandler {
  /**
   * On event.
   *
   * @param event the event
   * @throws InterruptedException the interrupted exception
   * @throws IOException          the io exception
   * @throws TimeoutException     the timeout exception
   */
  void handleEvent(Event event) throws InterruptedException, IOException, TimeoutException;

  /**
   * Get message topic to subscribe the handler to MessageSubscriber
   *
   * @return the topic to subscribe
   */
  String getTopicToSubscribe();
}
