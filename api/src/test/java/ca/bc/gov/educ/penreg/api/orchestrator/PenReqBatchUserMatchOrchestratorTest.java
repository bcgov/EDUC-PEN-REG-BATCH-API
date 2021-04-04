package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_MATCHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PenReqBatchUserMatchOrchestratorTest extends BaseOrchestratorTest {


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
  @Autowired
  private MessagePublisher messagePublisher;


  /**
   * The issue new pen orchestrator.
   */
  @Autowired
  private PenReqBatchUserMatchOrchestrator orchestrator;

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

  String studentID = UUID.randomUUID().toString();

  /**
   * Sets up.
   */
  @Before
  public void setUp() throws JsonProcessingException {
    MockitoAnnotations.openMocks(this);
    final var payload = this.placeholderPenRequestBatchActionsSagaData();
    this.sagaData = this.getPenRequestBatchUserActionsSagaDataFromJsonString(payload);
    this.sagaData.setAssignedPEN(TEST_PEN);
    this.sagaData.setLocalID("20345678");
    this.sagaData.setGradeCode("01");
    this.sagaData.setStudentID(this.studentID);
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
        UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));
  }


  @Test
  public void testGetStudentByPen_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT);
    assertThat(newEvent.getEventPayload()).isEqualTo(TEST_PEN);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GET_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testUpdateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var studentPayload = Student.builder().studentID(this.studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(GET_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_FOUND)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    final var student = JsonUtil.getJsonObjectFromString(Student.class, newEvent.getEventPayload());
    assertThat(student.getPen()).isEqualTo(TEST_PEN);
    assertThat(student.getMincode()).isEqualTo(this.mincode);
    assertThat(student.getLocalID()).isEqualTo("20345678");
    assertThat(student.getGradeCode()).isEqualTo("01");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(this.getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_FOUND.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithTwins_shouldPostEventToMatchApi() throws IOException, InterruptedException, TimeoutException {
    final var studentPayload = Student.builder().studentID(this.studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(UPDATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_UPDATED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(ADD_POSSIBLE_MATCH);
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<PossibleMatch> possibleMatches = objectMapper.readValue(newEvent.getEventPayload(), new TypeReference<>() {
    });
    assertThat(possibleMatches).size().isEqualTo(1);
    assertThat(possibleMatches.get(0).getStudentID()).isEqualTo(this.studentID);
    assertThat(possibleMatches.get(0).getMatchReasonCode()).isEqualTo(TwinReasonCodes.PEN_MATCH.getCode());
    assertThat(possibleMatches.get(0).getCreateUser()).isEqualTo("test");
    assertThat(possibleMatches.get(0).getUpdateUser()).isEqualTo("test");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(ADD_POSSIBLE_MATCH.toString());
    assertThat(this.getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_UPDATED.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithoutTwins_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    final var sagaFromDBtoUpdateOptional = this.sagaService.findSagaById(this.saga.getSagaId());
    if (sagaFromDBtoUpdateOptional.isPresent()) {
      final var sagaFromDBtoUpdate = sagaFromDBtoUpdateOptional.get();
      final var payload = JsonUtil.getJsonObjectFromString(PenRequestBatchUserActionsSagaData.class, sagaFromDBtoUpdate.getPayload());
      payload.setMatchedStudentIDList(null);
      sagaFromDBtoUpdate.setPayload(JsonUtil.getJsonStringFromObject(payload));
      this.sagaService.updateAttachedEntityDuringSagaProcess(sagaFromDBtoUpdate);
      this.saga = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    }
    final var studentPayload = Student.builder().studentID(this.studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(UPDATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_UPDATED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_MATCHED.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(this.studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(TEST_PEN);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(this.getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_UPDATED.toString());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var student = Student.builder().studentID(this.studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    final var event = Event.builder()
        .eventType(ADD_POSSIBLE_MATCH)
        .eventOutcome(EventOutcome.POSSIBLE_MATCH_ADDED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(student))
        .build();
    this.sagaData.setAssignedPEN(TEST_PEN);
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_MATCHED.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(this.studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(TEST_PEN);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(ADD_POSSIBLE_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.POSSIBLE_MATCH_ADDED.toString());
  }
}
