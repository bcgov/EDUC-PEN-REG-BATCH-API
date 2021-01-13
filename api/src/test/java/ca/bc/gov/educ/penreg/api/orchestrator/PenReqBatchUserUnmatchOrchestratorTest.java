package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenReqBatchUserUnmatchOrchestratorTest extends BaseOrchestratorTest {

  
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
   * The unmatch orchestrator.
   */
  @Autowired
  private PenReqBatchUserUnmatchOrchestrator orchestrator;

  /**
   * The Saga.
   */
  private Saga saga;
  /**
   * The Saga data.
   */
  private PenRequestBatchUnmatchSagaData sagaData;

  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  String studentID = UUID.randomUUID().toString();

  /**
   * The student twin id.
   */
  private final String studentTwinID = UUID.randomUUID().toString();

  /**
   * Sets up.
   */
  @Before
  public void setUp() throws JsonProcessingException {
    MockitoAnnotations.openMocks(this);
    var payload = placeholderPenRequestBatchUnmatchSagaData(Optional.of(studentTwinID));
    sagaData = getPenRequestBatchUnmatchSagaDataFromJsonString(payload);
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(sagaData),
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
  public void testHandleEvent_givenEventAndSagaDataWithTwins_shouldPostEventToPenMatchApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(sagaData))
        .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(DELETE_POSSIBLE_MATCH);
    List<Map<String, UUID>> payload = new ObjectMapper().readValue(newEvent.getEventPayload(), new TypeReference<>() {
    });
    assertThat(payload).size().isEqualTo(1);
    assertThat(payload.get(0)).containsKey("matchedStudentID").containsValue(UUID.fromString(studentTwinID));
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(DELETE_POSSIBLE_MATCH.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithoutTwins_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    var sagaFromDBtoUpdateOptional = sagaService.findSagaById(saga.getSagaId());
    if(sagaFromDBtoUpdateOptional.isPresent()){
      var sagaFromDBtoUpdate = sagaFromDBtoUpdateOptional.get();
      var payload = JsonUtil.getJsonObjectFromString(PenRequestBatchUnmatchSagaData.class, sagaFromDBtoUpdate.getPayload());
      payload.setMatchedStudentIDList(null);
      sagaFromDBtoUpdate.setPayload(JsonUtil.getJsonStringFromObject(payload));
      sagaService.updateAttachedEntityDuringSagaProcess(sagaFromDBtoUpdate);
      saga = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    }
    var payload = placeholderPenRequestBatchUnmatchSagaData(Optional.empty());
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(INITIATED)
                     .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(payload))
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    assertThat(prbStudent.getStudentID()).isNull();
    assertThat(prbStudent.getAssignedPEN()).isNull();
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var studentTwinIDs = List.of(studentTwinID);
    var event = Event.builder()
        .eventType(DELETE_POSSIBLE_MATCH)
        .eventOutcome(EventOutcome.POSSIBLE_MATCH_DELETED)
                     .sagaId(saga.getSagaId())
                     .eventPayload(JsonUtil.getJsonStringFromObject(studentTwinIDs))
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations+1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    assertThat(prbStudent.getStudentID()).isNull();
    assertThat(prbStudent.getAssignedPEN()).isNull();
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(DELETE_POSSIBLE_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.POSSIBLE_MATCH_DELETED.toString());
  }

  /**
   * Dummy pen request batch unmatch saga data json string.
   *
   * @return the string
   */
  protected String placeholderPenRequestBatchUnmatchSagaData(Optional<String> studentTwinID) {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"studentID\": \"" + UUID.randomUUID().toString() + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \"" + mincode + "\",\n" +
        "    \"genderCode\": \"X\",\n" +
        (studentTwinID.map(s -> "    \"matchedStudentIDList\": [\"" + s + "\"]\n").orElse("")) +
        "  }";
  }

  /**
   * Gets pen request batch unmatch saga data from json string.
   *
   * @param json the json
   * @return the pen request batch new pen saga data from json string
   */
  protected PenRequestBatchUnmatchSagaData getPenRequestBatchUnmatchSagaDataFromJsonString(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestBatchUnmatchSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
