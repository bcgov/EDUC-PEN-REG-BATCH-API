package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_NOT_FOUND;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Message publisher test.
 */
public class PenRequestBatchStudentEventServiceTest extends BasePenRegAPITest {
  @Autowired
  private PenRequestBatchEventRepository penRequestBatchEventRepository;
  @Autowired
  private PenRequestBatchStudentService prbStudentService;
  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;
  @Autowired
  private PenRequestBatchStudentEventService penRequestBatchStudentEventService;

  private String penRequestBatchID;

  private String penRequestBatchStudentID;

  private String payload;

  private final UUID sagaID = UUID.randomUUID();

  @Before
  public void setUp() {
    this.penRequestBatchID = UUID.randomUUID().toString();
    this.penRequestBatchStudentID = UUID.randomUUID().toString();
    this.payload = this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
  }


  @Test
  public void testUpdatePenRequestBatchStudent_givenNewSagaIdAndEventType_shouldUpdatePrbStudentAndReturnPEN_REQUEST_BATCH_STUDENT_UPDATED() throws IOException {
    final var batchList = PenRequestBatchTestUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json",
        "mock_pen_req_batch_student_archived.json", 1);
    this.penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    this.penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
        .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().get().getPenRequestBatchStudentID().toString();

    final var payload = this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    final var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, this.sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    final var penRequestBatchEvent = this.penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(this.sagaID);
    assertThat(penRequestBatchEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(penRequestBatchEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_UPDATED.toString());
    final var prbStudent = JsonUtil.getJsonObjectFromString(PenRequestBatchStudent.class, penRequestBatchEvent.getEventPayload());
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).contains(USR_NEW_PEN.toString());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(MESSAGE_PUBLISHED.toString());
    assertThat(penRequestBatchEvent.getReplyChannel()).isEqualTo(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());


    final var penRequestBatch = this.penRequestBatchRepository.findById(UUID.fromString(this.penRequestBatchID));
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);

    final var prbStudentEventEntity = this.penRequestBatchEventRepository.findBySagaId(this.sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenNewSagaIdAndEventType_and_NoPrbStudentRecord_shouldReturnPEN_REQUEST_BATCH_STUDENT_NOT_FOUND() throws IOException {
    final var payload = this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    final var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, this.sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    final var penRequestBatchEvent = this.penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(this.sagaID);
    assertThat(penRequestBatchEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(penRequestBatchEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_NOT_FOUND.toString());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(MESSAGE_PUBLISHED.toString());
    assertThat(penRequestBatchEvent.getReplyChannel()).isEqualTo(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());

    final var prbStudentEventEntity = this.penRequestBatchEventRepository.findBySagaId(this.sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenExistedSagaIdAndEventType_shouldReturnSamePenRequestBatchEvent() throws IOException {
    final var existedPrbEvent = PenRequestBatchEvent.builder().eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString())
        .eventOutcome(PEN_REQUEST_BATCH_STUDENT_UPDATED.toString()).eventStatus(MESSAGE_PUBLISHED.toString()).sagaId(this.sagaID).eventPayload(this.payload)
        .replyChannel(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString()).build();
    this.penRequestBatchEventRepository.save(existedPrbEvent);

    final var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, this.sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), this.payload);

    final var penRequestBatchEvent = this.penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(this.sagaID);
    assertThat(penRequestBatchEvent.getEventId()).isEqualTo(existedPrbEvent.getEventId());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(MESSAGE_PUBLISHED.toString());

    final var prbStudentEventEntity = this.penRequestBatchEventRepository.findBySagaId(this.sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
    assertThat(prbStudentEventEntity.get().getEventStatus()).isEqualTo(MESSAGE_PUBLISHED.toString());
  }

  protected String dummyPenRequestBatchStudentDataJson(final String status) {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"penRequestBatchStudentStatusCode\": \"" + status + "\",\n" +
        "    \"genderCode\": \"X\"\n" +
        "  }";
  }
}
