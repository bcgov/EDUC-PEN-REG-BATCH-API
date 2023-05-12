package ca.bc.gov.educ.penreg.api.orchestrator;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.STUDENT_ALREADY_EXIST;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.STUDENT_CREATED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.ADD_POSSIBLE_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.CREATE_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GET_NEXT_PEN_NUMBER;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_SERVICES_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The type Message publisher test.
 */
public class PenReqBatchNewPenOrchestratorTest extends BaseOrchestratorTest {
  /**
   * The Repository.
   */
  @Autowired
  SagaRepository repository;
  /**
   * The Saga event repository.
   */
  @Autowired
  SagaEventRepository sagaEventRepository;
  /**
   * The Saga service.
   */
  @Autowired
  private SagaService sagaService;

  /**
   * The Message publisher.
   */
  @Mock
  private MessagePublisher messagePublisher;



  /**
   * The issue new pen orchestrator.
   */
  private PenReqBatchNewPenOrchestrator orchestrator;

  /**
   * The Saga.
   */
  private Saga saga;
  /**
   * The Saga data.
   */
  private PenRequestBatchUserActionsSagaData sagaData;

  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;


  @Autowired
  RestUtils restUtils;
  /**
   * Sets up.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    this.orchestrator = new PenReqBatchNewPenOrchestrator(this.sagaService, this.messagePublisher, restUtils);
    final var payload = this.placeholderPenRequestBatchActionsSagaData();
    this.sagaData = this.getPenRequestBatchUserActionsSagaDataFromJsonString(payload);
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload,
        UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));
  }


  /**
   * Test get next pen number.
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testGetNextPenNumber_givenEventAndSagaData_shouldPostEventToServicesApi() throws JsonProcessingException {
    final var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.getNextPenNumber(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_SERVICES_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_NEXT_PEN_NUMBER);
    assertThat(newEvent.getEventPayload()).isEqualTo(this.saga.getSagaId().toString());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GET_NEXT_PEN_NUMBER.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  /**
   * Test create student.
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws JsonProcessingException {
    final var pen = "123456789";
    final var event = Event.builder()
        .eventType(GET_NEXT_PEN_NUMBER)
        .eventOutcome(EventOutcome.NEXT_PEN_NUMBER_RETRIEVED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(pen)
        .build();
    this.orchestrator.createStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    final var student = JsonUtil.getJsonObjectFromString(Student.class, newEvent.getEventPayload());
    assertThat(student.getPen()).isEqualTo(pen);
    assertThat(student.getDemogCode()).isEqualTo("A");
    assertThat(student.getStatusCode()).isEqualTo("A");
    assertThat(student.getMincode()).isEqualTo(this.mincode);
    assertThat(student.getGenderCode()).isEqualTo("X");
    assertThat(student.getSexCode()).isEqualTo("U");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    assertThat(this.getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(pen);
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_NEXT_PEN_NUMBER.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.NEXT_PEN_NUMBER_RETRIEVED.toString());
  }

  /**
   * Test update pen request batch student.
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws JsonProcessingException {
    final var pen = "123456789";
    final var studentID = UUID.randomUUID().toString();
    final var student = Student.builder().studentID(studentID).pen(pen).legalFirstName("Jack").build();
    final var event = Event.builder()
        .eventType(CREATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_CREATED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(student))
        .build();
    this.sagaData.setAssignedPEN(pen);
    this.orchestrator.updatePenRequestBatchStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(pen);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_CREATED.toString());
  }

  /**
   * Test update pen request batch student
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaDataWithNullStudentID_AndNoCreateStudentEventRecord_shouldThrowException() throws JsonProcessingException {
    final var pen = "123456789";
    final var studentID = UUID.randomUUID().toString();
    final var student = Student.builder().studentID(studentID).pen(pen).legalFirstName("Jack").build();
    final var event = Event.builder()
      .eventType(ADD_POSSIBLE_MATCH)
      .eventOutcome(EventOutcome.POSSIBLE_MATCH_ADDED)
      .sagaId(this.saga.getSagaId())
      .eventPayload(JsonUtil.getJsonStringFromObject(student))
      .build();
    this.sagaData.setAssignedPEN(pen);
    Exception exception = assertThrows(PenRegAPIRuntimeException.class, () -> {
      this.orchestrator.updatePenRequestBatchStudent(event, this.saga, this.sagaData);
    });
    assertThat(exception.getMessage()).contains("CREATE_STUDENT event not found in event states table for saga id");
  }

  /**
   * Test update pen request batch student.
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaDataWithNullStudentID_AndStudentCreatedEventRecord_shouldPostEventToBatchApi() throws JsonProcessingException {
    final var pen = "123456789";
    final var studentID = UUID.randomUUID().toString();
    final var student = Student.builder().studentID(studentID).pen(pen).legalFirstName("Jack").build();
    final var event = Event.builder()
      .eventType(ADD_POSSIBLE_MATCH)
      .eventOutcome(EventOutcome.POSSIBLE_MATCH_ADDED)
      .sagaId(this.saga.getSagaId())
      .eventPayload(JsonUtil.getJsonStringFromObject(student))
      .build();
    this.sagaData.setAssignedPEN(pen);
    final var sagaEvent = SagaEvent.builder()
      .saga(this.saga)
      .sagaEventState(CREATE_STUDENT.toString())
      .sagaEventOutcome(STUDENT_CREATED.toString())
      .sagaStepNumber(3)
      .sagaEventResponse(JsonUtil.getJsonStringFromObject(student))
      .createDate(LocalDateTime.now())
      .createUser("Test")
      .updateUser("Test")
      .updateDate(LocalDateTime.now())
      .build();
    this.sagaEventRepository.save(sagaEvent);
    this.orchestrator.updatePenRequestBatchStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(pen);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.ADD_POSSIBLE_MATCH.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.POSSIBLE_MATCH_ADDED.toString());
  }

  /**
   * Test update pen request batch student.
   *
   * @throws JsonProcessingException the json processing exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaDataWithNullStudentID_AndStudentAlreadyExistEventRecord_shouldPostEventToBatchApi() throws JsonProcessingException {
    final var pen = "123456789";
    final var studentID = UUID.randomUUID().toString();
    final var student = Student.builder().studentID(studentID).pen(pen).legalFirstName("Jack").build();
    final var event = Event.builder()
      .eventType(ADD_POSSIBLE_MATCH)
      .eventOutcome(EventOutcome.POSSIBLE_MATCH_ADDED)
      .sagaId(this.saga.getSagaId())
      .eventPayload(JsonUtil.getJsonStringFromObject(student))
      .build();
    this.sagaData.setAssignedPEN(pen);
    final var sagaEvent = SagaEvent.builder()
      .saga(this.saga)
      .sagaEventState(CREATE_STUDENT.toString())
      .sagaEventOutcome(STUDENT_ALREADY_EXIST.toString())
      .sagaStepNumber(3)
      .sagaEventResponse(student.getStudentID())
      .createDate(LocalDateTime.now())
      .createUser("Test")
      .updateUser("Test")
      .updateDate(LocalDateTime.now())
      .build();
    this.sagaEventRepository.save(sagaEvent);
    this.orchestrator.updatePenRequestBatchStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atMostOnce()).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(pen);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.ADD_POSSIBLE_MATCH.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.POSSIBLE_MATCH_ADDED.toString());
  }

}
