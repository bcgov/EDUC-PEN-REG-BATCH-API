package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.model.v1.Saga;

import java.io.IOException;
import java.util.List;
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
   * @param userName                 the user who created the saga
   * @return the saga
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  Saga startSaga(String payload, UUID penRequestBatchStudentID, UUID penRequestBatchID, String userName) throws InterruptedException, TimeoutException, IOException;


  List<Saga> saveMultipleSagas(String payload, List<UUID> penRequestBatchIDs, String userName) throws InterruptedException, TimeoutException, IOException;

  void startMultipleSagas(List<Saga> sagas) throws InterruptedException, TimeoutException, IOException;

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
