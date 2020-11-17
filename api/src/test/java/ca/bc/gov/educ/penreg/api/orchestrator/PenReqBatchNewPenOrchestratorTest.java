package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
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
import org.mockito.MockitoAnnotations;
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

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
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


  /**
   * Sets up.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orchestrator = new PenReqBatchNewPenOrchestrator(sagaService, messagePublisher);
    var payload = placeholderPenRequestBatchActionsSagaData();
    sagaData = getPenRequestBatchUserActionsSagaDataFromJsonString(payload);
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload,
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

  /**
   * Test get next pen number.
   *
   * @throws JsonProcessingException the json processing exception
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
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GET_NEXT_PEN_NUMBER.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
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
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    assertThat(getPenRequestBatchUserActionsSagaDataFromJsonString(currentSaga.getPayload()).getAssignedPEN()).isEqualTo(pen);
    var sagaStates = sagaService.findAllSagaStates(saga);
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
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_CREATED.toString());
  }

 

}
