package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchStudentOrchestrator;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
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
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.READ_FROM_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    this.eventPublisherService = new EventPublisherService(this.messagePublisher);
    this.eventHandlerService = new EventHandlerService(this.sagaService, this.penReqBatchStudentOrchestrator,
        this.penRequestBatchEventRepository, this.prbStudentEventService, this.eventPublisherService);

    this.penRequestBatchID = UUID.randomUUID().toString();
    this.penRequestBatchStudentID = UUID.randomUUID().toString();
  }

  @After
  public void after() {
    this.sagaEventRepository.deleteAll();
    this.repository.deleteAll();
    this.penRequestBatchEventRepository.deleteAll();
    this.penRequestBatchRepository.deleteAll();
  }


  @Test
  public void testHandleEvent_givenEventTypeREAD_FROM_TOPIC_shouldStartPenRequestBatchStudentSaga() throws InterruptedException, IOException, TimeoutException {
    final var payload = this.dummyPenRequestBatchStudentSagaDataJson();
    final var event = new Event(READ_FROM_TOPIC, null, null, null, payload);
    this.eventHandlerService.handleEvent(event);
    verify(this.penReqBatchStudentOrchestrator, atMostOnce()).startSaga(payload, UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID), ApplicationProperties.API_NAME);
  }

  /**
   * Test handle duplicate READ_FROM_TOPIC event.
   *
   */
  @Test
  public void testHandleEvent_givenDuplicateEventTypeREAD_FROM_TOPIC_shouldNotStartPenRequestBatchStudentSaga() {
    final var payload = this.dummyPenRequestBatchStudentSagaDataJson();
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", payload,
        UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));
    final var event = new Event(READ_FROM_TOPIC, null, null, null, payload);
    this.eventHandlerService.handleEvent(event);
    verifyNoMoreInteractions(this.penReqBatchStudentOrchestrator);
  }

  /**
   * Test handle UPDATE_PEN_REQUEST_BATCH_STUDENT event.
   *
   */
  @Test
  public void testHandleEvent_givenEventTypeUPDATE_PEN_REQUEST_BATCH_STUDENT_shouldUpdatePrbStudentAndSendEvent() throws IOException {
    final var sagaID = UUID.randomUUID();
    final var batchList = PenRequestBatchUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json",
        "mock_pen_req_batch_student_archived.json", 1);
    this.penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    this.penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
        .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().orElseThrow().getPenRequestBatchStudentID().toString();
    final var payload = this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    final var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);

    this.eventHandlerService.handleEvent(event);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString()), this.eventCaptor.capture());

    final var replyEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(replyEvent.getSagaId()).isEqualTo(sagaID);
    assertThat(replyEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    assertThat(replyEvent.getEventOutcome()).isEqualTo(PEN_REQUEST_BATCH_STUDENT_UPDATED);
    assertThat(replyEvent.getEventPayload()).contains(USR_NEW_PEN.toString());

    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());

    final var penRequestBatch = this.penRequestBatchRepository.findById(UUID.fromString(this.penRequestBatchID));
    assertThat(penRequestBatch.orElseThrow().getNewPenCount()).isEqualTo(3);
  }

  /**
   * Test handle UPDATE_PEN_REQUEST_BATCH_STUDENT event.
   *
   */
  @Test
  public void testHandleEvent_givenEventTypeUPDATE_PEN_REQUEST_BATCH_STUDENT_and_FailedToSendEvent_shouldUpdatePrbStudent() throws IOException {
    final var sagaID = UUID.randomUUID();
    final var batchList = PenRequestBatchUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json",
        "mock_pen_req_batch_student_archived.json", 1);
    this.penRequestBatchID = batchList.get(0).getPenRequestBatchID().toString();
    this.penRequestBatchStudentID = batchList.get(0).getPenRequestBatchStudentEntities().stream()
        .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(FIXABLE.getCode())).findFirst().get().getPenRequestBatchStudentID().toString();
    final var payload = this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString());
    final var event = new Event(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED, sagaID, PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), payload);
    doThrow(new RuntimeException("Test")).when(this.messagePublisher).dispatchMessage(anyString(), any());

    this.eventHandlerService.handleEvent(event);

    final var penRequestBatch = this.penRequestBatchRepository.findById(UUID.fromString(this.penRequestBatchID));
    assertThat(penRequestBatch.orElseThrow().getNewPenCount()).isEqualTo(3);
  }

  protected String dummyPenRequestBatchStudentSagaDataJson() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \"" + this.mincode + "\",\n" +
        "    \"genderCode\": \"X\"\n" +
        "  }";
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
