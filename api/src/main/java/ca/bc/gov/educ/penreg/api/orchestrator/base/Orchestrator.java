package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.model.Saga;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * The interface Orchestrator.
 */
public interface Orchestrator {

  /**
   * Start saga saga.
   *
   * @param payload                  the payload
   * @param penRequestBatchStudentID the pen request batch student id
   * @param penRequestBatchID        the pen request batch id
   * @return the saga
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  Saga startSaga(String payload, UUID penRequestBatchStudentID, UUID penRequestBatchID) throws InterruptedException, TimeoutException, IOException;

  /**
   * Gets saga name.
   *
   * @return the saga name
   */
  String getSagaName();

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(Saga saga) throws IOException, InterruptedException, TimeoutException;
}