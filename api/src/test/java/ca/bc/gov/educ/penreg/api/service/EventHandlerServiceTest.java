package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchStudentOrchestrator;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.DB_COMMITTED;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class EventHandlerServiceTest {
  @Autowired
  private SagaRepository repository;
  @Autowired
  private SagaEventRepository sagaEventRepository;
  @Autowired
  private SagaService sagaService;
  @Autowired
  private PenRequestBatchEventRepository penRequestBatchEventRepository;
  @Autowired
  private PenRequestBatchStudentService prbStudentService;
  @Autowired
  private PenRequestBatchStudentEventService prbStudentEventService;
  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Mock
  private PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator;
  @Mock
  private MessagePublisher messagePublisher;

  private EventPublisherService eventPublisherService;

  private String penRequestBatchID;

  private String penRequestBatchStudentID;

  private final String mincode = "01292001";

  private EventHandlerService eventHandlerService;

  @Captor
  ArgumentCaptor<byte[]> eventCaptor;


  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    eventPublisherService = new EventPublisherService(messagePublisher);
    eventHandlerService = new EventHandlerService(sagaService, penReqBatchStudentOrchestrator,
      penRequestBatchEventRepository, prbStudentEventService, eventPublisherService);

    penRequestBatchID = UUID.randomUUID().toString();
    penRequestBatchStudentID = UUID.randomUUID().toString();
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
    penRequestBatchEventRepository.deleteAll();
    penRequestBatchRepository.deleteAll();
  }

  @Test
  public void testHandleEvent_givenEventTypeSTUDENT_EVENT_OUTBOX_PROCESSED_shouldUpdateDBStatus() {
    var prbEvent = PenRequestBatchEvent.builder().eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString())
      .eventOutcome(PEN_REQUEST_BATCH_STUDENT_UPDATED.toString()).eventStatus(DB_COMMITTED.toString()).
      eventPayload("{}").createDate(LocalDateTime.now()).createUser("TEST").build();
    penRequestBatchEventRepository.save(prbEvent);
    var eventId = prbEvent.getEventId();
    var event = new Event(PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED, null, null, null, eventId.toString());
    eventHandlerService.handleEvent(event);
    var prbEventUpdated = penRequestBatchEventRepository.findById(eventId);
    assertThat(prbEventUpdated).isPresent();
    assertThat(prbEventUpdated.get().getEventStatus()).isEqualTo(MESSAGE_PUBLISHED.toString());
  }

  @Test
  public void testHandleEvent_givenEventTypeREAD_FROM_TOPIC_shouldStartPenRequestBatchStudentSaga() throws InterruptedException, IOException, TimeoutException {
    var payload = dummyPenRequestBatchStudentSagaDataJson();
    var event = new Event(READ_FROM_TOPIC, null, null, null, payload);
    eventHandlerService.handleEvent(event);
    verify(penReqBatchStudentOrchestrator, atMostOnce()).startSaga(payload, UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));
  }

  /**
   * Test handle duplicate READ_FROM_TOPIC event.
   *
   */
  @Test
  public void testHandleEvent_givenDuplicateEventTypeREAD_FROM_TOPIC_shouldNotStartPenRequestBatchStudentSaga() {
    var payload = dummyPenRequestBatchStudentSagaDataJson();
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", payload,
      UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));
    var event = new Event(READ_FROM_TOPIC, null, null, null, payload);
    eventHandlerService.handleEvent(event);
    verifyNoMoreInteractions(penReqBatchStudentOrchestrator);
  }

  /**
   * Test handle UPDATE_PEN_REQUEST_BATCH_STUDENT event.
   *
   */
  @Test
  public void testHandleEvent_givenEventTypeUPDATE_PEN_REQUEST_BATCH_STUDENT_shouldUpdatePrbStudentAndSendEvent() throws IOException {
    var sagaID = UUID.randomUUID();
    var batchList = PenRequestBatchUtils.createBatchStudents(penRequestBatchRepository, "mock_pen_req_batch_archived.json",
      "mock_pen_req_batch_student_archived.json", 1);
    penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
      .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().orElseThrow().getPenRequestBatchStudentID().toString();
    var payload = dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    eventHandlerService.handleEvent(event);
    verify(messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString()), eventCaptor.capture());

    var replyEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(replyEvent.getSagaId()).isEqualTo(sagaID);
    assertThat(replyEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    assertThat(replyEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_UPDATED);
    assertThat(replyEvent.getEventPayload()).contains(USR_NEW_PEN.toString());

    verify(messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var outboxEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(outboxEvent.getEventType()).isEqualTo(PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED);

    var penRequestBatch = penRequestBatchRepository.findById(UUID.fromString(penRequestBatchID));
    assertThat(penRequestBatch.orElseThrow().getNewPenCount()).isEqualTo(3);
  }

  /**
   * Test handle UPDATE_PEN_REQUEST_BATCH_STUDENT event.
   *
   */
  @Test
  public void testHandleEvent_givenEventTypeUPDATE_PEN_REQUEST_BATCH_STUDENT_and_FailedToSendEvent_shouldUpdatePrbStudent() throws IOException {
    var sagaID = UUID.randomUUID();
    var batchList = PenRequestBatchUtils.createBatchStudents(penRequestBatchRepository, "mock_pen_req_batch_archived.json",
      "mock_pen_req_batch_student_archived.json", 1);
    penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
      .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().get().getPenRequestBatchStudentID().toString();
    var payload = dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);
    doThrow(new RuntimeException("Test")).when(messagePublisher).dispatchMessage(anyString(), any());

    eventHandlerService.handleEvent(event);

    var penRequestBatch = penRequestBatchRepository.findById(UUID.fromString(penRequestBatchID));
    assertThat(penRequestBatch.orElseThrow().getNewPenCount()).isEqualTo(3);
  }

  protected String dummyPenRequestBatchStudentSagaDataJson() {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"mincode\": \""+ mincode + "\",\n" +
      "    \"genderCode\": \"X\"\n" +
      "  }";
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
