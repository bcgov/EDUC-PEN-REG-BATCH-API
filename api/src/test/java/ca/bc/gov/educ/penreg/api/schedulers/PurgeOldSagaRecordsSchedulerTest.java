package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PurgeOldSagaRecordsSchedulerTest {

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;


  @Test
  public void pollSagaTableAndPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    String penRequestBatchID = "7f000101-7151-1d84-8171-5187006c0000";
    String getPenRequestBatchStudentID = "7f000101-7151-1d84-8171-5187006c0001";
    var payload = " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + getPenRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\"\n" +
        "  }";
    var saga = getSaga(payload);
    repository.save(saga);
    sagaEventRepository.save(getSagaEvent(saga,payload));
    purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    var sagas = repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private Saga getSaga(String payload) {
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
  private SagaEvent getSagaEvent(Saga saga, String payload) {
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