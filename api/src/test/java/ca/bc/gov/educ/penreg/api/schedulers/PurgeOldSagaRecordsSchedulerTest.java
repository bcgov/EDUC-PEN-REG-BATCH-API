package ca.bc.gov.educ.penreg.api.schedulers;

import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.constants.EventStatus;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PurgeOldSagaRecordsSchedulerTest extends BasePenRegAPITest {

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PenRequestBatchEventRepository penRequestBatchEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;

  @Before
  public void setup() {
    this.sagaEventRepository.deleteAll();
    this.penRequestBatchEventRepository.deleteAll();
    this.repository.deleteAll();
  }

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
    final var saga_today = this.getSaga(payload, LocalDateTime.now());
    final var yesterday = LocalDateTime.now().minusDays(1);
    final var saga_yesterday = this.getSaga(payload, yesterday);

    this.repository.save(saga_today);
    this.sagaEventRepository.save(this.getSagaEvent(saga_today, payload));
    this.penRequestBatchEventRepository.save(this.getPenRequestBatchEvent(saga_today, payload, LocalDateTime.now()));

    this.repository.save(saga_yesterday);
    this.sagaEventRepository.save(this.getSagaEvent(saga_yesterday, payload));
    this.penRequestBatchEventRepository.save(this.getPenRequestBatchEvent(saga_yesterday, payload, yesterday));

    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(1);
    this.purgeOldSagaRecordsScheduler.purgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).hasSize(1);

    final var sagaEvents = this.sagaEventRepository.findAll();
    assertThat(sagaEvents).hasSize(1);

    final var servicesEvents = this.penRequestBatchEventRepository.findAll();
    assertThat(servicesEvents).hasSize(1);
  }


  private Saga getSaga(final String payload, final LocalDateTime createDateTime) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName("PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA")
        .status(COMPLETED.toString())
        .sagaState(COMPLETED.toString())
        .createDate(createDateTime)
        .createUser("PEN_REG_BATCH_API")
        .updateUser("PEN_REG_BATCH_API")
        .updateDate(createDateTime)
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

  private PenRequestBatchEvent getPenRequestBatchEvent(final Saga saga, final String payload, final LocalDateTime createDateTime) {
    return PenRequestBatchEvent
      .builder()
      .eventPayloadBytes(payload.getBytes())
      .eventStatus(EventStatus.MESSAGE_PUBLISHED.toString())
      .eventType("UPDATE_STUDENT")
      .sagaId(saga.getSagaId())
      .eventOutcome("STUDENT_UPDATED")
      .replyChannel("TEST_CHANNEL")
      .createDate(createDateTime)
      .createUser("PEN_REG_BATCH_API")
      .updateUser("PEN_REG_BATCH_API")
      .updateDate(createDateTime)
      .build();
  }
}
