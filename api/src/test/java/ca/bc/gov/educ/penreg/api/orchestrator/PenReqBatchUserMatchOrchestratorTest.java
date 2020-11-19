package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.StudentTwin;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_MATCHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
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
    var payload = placeholderPenRequestBatchActionsSagaData();
    sagaData = getPenRequestBatchUserActionsSagaDataFromJsonString(payload);
    sagaData.setAssignedPEN(TEST_PEN);
    sagaData.setLocalID("20345678");
    sagaData.setGradeCode("01");
    sagaData.setStudentID(studentID);
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(sagaData),
        UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));
  }

  /**
   * After.
   */
  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testGetStudentByPen_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(EventType.INITIATED)
                     .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT);
    assertThat(newEvent.getEventPayload()).isEqualTo(TEST_PEN);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GET_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testUpdateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var studentPayload = Student.builder().studentID(studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(GET_STUDENT)
                     .eventOutcome(EventOutcome.STUDENT_FOUND)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    var student = JsonUtil.getJsonObjectFromString(Student.class, newEvent.getEventPayload());
    assertThat(student.getPen()).isEqualTo(TEST_PEN);
    assertThat(student.getMincode()).isEqualTo(mincode);
    assertThat(student.getLocalID()).isEqualTo("20345678");
    assertThat(student.getGradeCode()).isEqualTo("01");
    assertThat(student.getStudentTwinAssociations()).isNull();
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_FOUND.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithTwins_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var studentPayload = Student.builder().studentID(studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(UPDATE_STUDENT)
                     .eventOutcome(EventOutcome.STUDENT_UPDATED)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(ADD_STUDENT_TWINS);
    final ObjectMapper objectMapper = new ObjectMapper();
    CollectionType javaType = objectMapper.getTypeFactory()
                                          .constructCollectionType(List.class, StudentTwin.class);
    List<StudentTwin> studentTwins = objectMapper.readValue(newEvent.getEventPayload(), javaType);
    assertThat(studentTwins).size().isEqualTo(1);
    assertThat(studentTwins.get(0).getStudentID()).isEqualTo(studentID);
    assertThat(studentTwins.get(0).getStudentTwinReasonCode()).isEqualTo(TwinReasonCodes.PEN_MATCH.getCode());
    assertThat(studentTwins.get(0).getCreateUser()).isEqualTo("test");
    assertThat(studentTwins.get(0).getUpdateUser()).isEqualTo("test");
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(ADD_STUDENT_TWINS.toString());
    assertThat(getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_UPDATED.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithoutTwins_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    var sagaFromDBtoUpdateOptional = sagaService.findSagaById(saga.getSagaId());
    if(sagaFromDBtoUpdateOptional.isPresent()){
      var sagaFromDBtoUpdate = sagaFromDBtoUpdateOptional.get();
      var payload = JsonUtil.getJsonObjectFromString(PenRequestBatchUserActionsSagaData.class, sagaFromDBtoUpdate.getPayload());
      payload.setTwinStudentIDs(null);
      sagaFromDBtoUpdate.setPayload(JsonUtil.getJsonStringFromObject(payload));
      sagaService.updateAttachedEntityDuringSagaProcess(sagaFromDBtoUpdate);
      saga = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    }
    var studentPayload = Student.builder().studentID(studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(UPDATE_STUDENT)
                     .eventOutcome(EventOutcome.STUDENT_UPDATED)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(studentPayload))
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_MATCHED.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(TEST_PEN);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(TEST_PEN);
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_UPDATED.toString());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var student = Student.builder().studentID(studentID).pen(TEST_PEN).legalFirstName("Jack").build();
    var event = Event.builder()
                     .eventType(ADD_STUDENT_TWINS)
                     .eventOutcome(EventOutcome.STUDENT_TWINS_ADDED)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(student))
                     .build();
    sagaData.setAssignedPEN(TEST_PEN);
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_MATCHED.getCode());
    assertThat(prbStudent.getStudentID()).isEqualTo(studentID);
    assertThat(prbStudent.getAssignedPEN()).isEqualTo(TEST_PEN);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.ADD_STUDENT_TWINS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_TWINS_ADDED.toString());
  }
}
