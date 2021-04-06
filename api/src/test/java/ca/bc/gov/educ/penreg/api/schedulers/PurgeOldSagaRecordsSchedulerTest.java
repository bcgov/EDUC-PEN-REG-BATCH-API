package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

public class PurgeOldSagaRecordsSchedulerTest extends BasePenRegAPITest {

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;


  @Test
  public void pollSagaTableAndPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    final String penRequestBatchID = "7f000101-7151-1d84-8171-5187006c0000";
    final String getPenRequestBatchStudentID = "7f000101-7151-1d84-8171-5187006c0001";
    final var payload = " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + getPenRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\"\n" +
        "  }";
    final var saga = this.getSaga(payload);
    this.repository.save(saga);
    this.sagaEventRepository.save(this.getSagaEvent(saga, payload));
    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    this.purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private Saga getSaga(final String payload) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName("PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA")
        .status(COMPLETED.toString())
        .sagaState(COMPLETED.toString())
        .createDate(LocalDateTime.now())
        .createUser("PEN_REG_BATCH_API")
        .updateUser("PEN_REG_BATCH_API")
        .updateDate(LocalDateTime.now())
        .build();
  }

  private SagaEvent getSagaEvent(final Saga saga, final String payload) {
    return SagaEvent
        .builder()
        .sagaEventResponse(payload)
        .saga(saga)
        .sagaEventState("UPDATE_PEN_REQUEST_BATCH_STUDENT")
        .sagaStepNumber(3)
        .sagaEventOutcome("PEN_REQUEST_BATCH_STUDENT_UPDATED")
        .createDate(LocalDateTime.now())
        .createUser("PEN_REG_BATCH_API")
        .updateUser("PEN_REG_BATCH_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
}
