package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchNewPenSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes.PENCREATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenReqBatchNewPenOrchestratorTest {
  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;
  @Autowired
  private SagaService sagaService;

  @Mock
  private MessagePublisher messagePublisher;
  @Mock
  private MessageSubscriber messageSubscriber;
  @Mock
  private EventTaskSchedulerAsyncService taskSchedulerService;

  private final String penRequestBatchID = UUID.randomUUID().toString();

  private final String penRequestBatchStudentID = UUID.randomUUID().toString();

  private final String twinStudentID = UUID.randomUUID().toString();

  private final String mincode = "01292001";

  /**
   * The issue new pen orchestrator.
   */
  private PenReqBatchNewPenOrchestrator orchestrator;

  private Saga saga;
  private PenRequestBatchNewPenSagaData sagaData;

  @Captor
  ArgumentCaptor<byte[]> eventCaptor;


  @Before
  public void setUp() {
    initMocks(this);
    orchestrator = new PenReqBatchNewPenOrchestrator(sagaService, messagePublisher, messageSubscriber, taskSchedulerService);
    var payload = dummyPenRequestBatchNewPenSagaDataJson();
    sagaData = getPenRequestBatchNewPenSagaDataFromJsonString(payload);
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload,
      UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  /**
   * Test get next pen number.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetNextPenNumber_givenEventAndSagaData_shouldPostEventToServicesApi() throws JsonProcessingException {
    var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(saga.getSagaId())
      .build();
    orchestrator.getNextPenNumber(event, saga, sagaData);
    verify(messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_SERVICES_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_NEXT_PEN_NUMBER);
    assertThat(newEvent.getEventPayload()).isEqualTo(saga.getSagaId().toString());

    assertThat(sagaService.findSagaById(saga.getSagaId()).get().getSagaState()).isEqualTo(GET_NEXT_PEN_NUMBER.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  /**
   * Test create student.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws JsonProcessingException {
    var pen = "123456789";
    var event = Event.builder()
      .eventType(GET_NEXT_PEN_NUMBER)
      .eventOutcome(EventOutcome.NEXT_PEN_NUMBER_RETRIEVED)
      .sagaId(saga.getSagaId())
      .eventPayload(pen)
      .build();
    orchestrator.createStudent(event, saga, sagaData);
    verify(messagePublisher, atMostOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    var student = JsonUtil.getJsonObjectFromString(Student.class, newEvent.getEventPayload());
    assertThat(student.getPen()).isEqualTo(pen);
    assertThat(student.getDemogCode()).isEqualTo("A");
    assertThat(student.getStatusCode()).isEqualTo("A");
    assertThat(student.getMincode()).isEqualTo(mincode);
    assertThat(student.getGenderCode()).isEqualTo("X");
    assertThat(student.getSexCode()).isEqualTo("U");
    assertThat(student.getStudentTwinAssociations().size()).isEqualTo(1);
    assertThat(student.getStudentTwinAssociations().get(0).getTwinStudentID()).isEqualTo(twinStudentID);
    assertThat(student.getStudentTwinAssociations().get(0).getStudentTwinReasonCode()).isEqualTo(PENCREATE.getCode());

    var currentSaga = sagaService.findSagaById(saga.getSagaId()).get();
    assertThat(currentSaga.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    assertThat(getPenRequestBatchNewPenSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(pen);
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_NEXT_PEN_NUMBER.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.NEXT_PEN_NUMBER_RETRIEVED.toString());
  }

  /**
   * Test update pen request batch student.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws JsonProcessingException {
    var pen = "123456789";
    var studentID = UUID.randomUUID().toString();
    var student = Student.builder().studentID(studentID).pen(pen).legalFirstName("Jack").build();
    var event = Event.builder()
      .eventType(CREATE_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_CREATED)
      .sagaId(saga.getSagaId())
      .eventPayload(JsonUtil.getJsonStringFromObject(student))
      .build();
    sagaData.setAssignedPEN(pen);
    orchestrator.updatePenRequestBatchStudent(event, saga, sagaData);
    verify(messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(pen);

    var currentSaga = sagaService.findSagaById(saga.getSagaId()).get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_CREATED.toString());
  }

  protected String dummyPenRequestBatchNewPenSagaDataJson() {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"mincode\": \""+ mincode + "\",\n" +
      "    \"genderCode\": \"X\",\n" +
      "    \"twinStudentIDs\": [\"" + twinStudentID + "\"]\n" +
      "  }";
  }

  protected PenRequestBatchNewPenSagaData getPenRequestBatchNewPenSagaDataFromJsonString(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestBatchNewPenSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
