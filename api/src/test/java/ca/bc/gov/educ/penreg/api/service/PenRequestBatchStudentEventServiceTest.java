package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_NOT_FOUND;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.DB_COMMITTED;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenRequestBatchStudentEventServiceTest {
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
    penRequestBatchID = UUID.randomUUID().toString();
    penRequestBatchStudentID = UUID.randomUUID().toString();
    payload = dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
  }

  @After
  public void after() {
    penRequestBatchEventRepository.deleteAll();
    penRequestBatchRepository.deleteAll();
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenNewSagaIdAndEventType_shouldUpdatePrbStudentAndReturnPEN_REQUEST_BATCH_STUDENT_UPDATED() throws IOException {
    var batchList = PenRequestBatchUtils.createBatchStudents(penRequestBatchRepository, "mock_pen_req_batch_archived.json",
      "mock_pen_req_batch_student_archived.json", 1);
    penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
      .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().get().getPenRequestBatchStudentID().toString();

    var payload = dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    var penRequestBatchEvent = penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(sagaID);
    assertThat(penRequestBatchEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(penRequestBatchEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_UPDATED.toString());
    var prbStudent = JsonUtil.getJsonObjectFromString(PenRequestBatchStudent.class, penRequestBatchEvent.getEventPayload());
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).contains(USR_NEW_PEN.toString());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(DB_COMMITTED.toString());
    assertThat(penRequestBatchEvent.getReplyChannel()).isEqualTo(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());


    var penRequestBatch = penRequestBatchRepository.findById(UUID.fromString(penRequestBatchID));
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);

    var prbStudentEventEntity = penRequestBatchEventRepository.findBySagaId(sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenNewSagaIdAndEventType_and_NoPrbStudentRecord_shouldReturnPEN_REQUEST_BATCH_STUDENT_NOT_FOUND() throws IOException {
    var payload = dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    var penRequestBatchEvent = penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(sagaID);
    assertThat(penRequestBatchEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(penRequestBatchEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_NOT_FOUND.toString());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(DB_COMMITTED.toString());
    assertThat(penRequestBatchEvent.getReplyChannel()).isEqualTo(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());

    var prbStudentEventEntity = penRequestBatchEventRepository.findBySagaId(sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenExistedSagaIdAndEventType_shouldReturnSamePenRequestBatchEvent() throws IOException {
    var existedPrbEvent = PenRequestBatchEvent.builder().eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString())
      .eventOutcome(PEN_REQUEST_BATCH_STUDENT_UPDATED.toString()).eventStatus(MESSAGE_PUBLISHED.toString()).sagaId(sagaID).eventPayload(payload)
      .replyChannel(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString()).build();
    penRequestBatchEventRepository.save(existedPrbEvent);

    var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, null, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    var penRequestBatchEvent = penRequestBatchStudentEventService.updatePenRequestBatchStudent(event);

    assertThat(penRequestBatchEvent.getSagaId()).isEqualTo(sagaID);
    assertThat(penRequestBatchEvent.getEventId()).isEqualTo(existedPrbEvent.getEventId());
    assertThat(penRequestBatchEvent.getEventStatus()).isEqualTo(DB_COMMITTED.toString());

    var prbStudentEventEntity = penRequestBatchEventRepository.findBySagaId(sagaID);
    assertThat(prbStudentEventEntity.get().getEventId()).isEqualTo(penRequestBatchEvent.getEventId());
    assertThat(prbStudentEventEntity.get().getEventStatus()).isEqualTo(DB_COMMITTED.toString());
  }

  protected String dummyPenRequestBatchStudentDataJson(String status) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"penRequestBatchStudentStatusCode\": \"" + status + "\",\n" +
      "    \"genderCode\": \"X\"\n" +
      "  }";
  }
}
