package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
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
    final var payload = this.placeholderPenRequestBatchUnmatchSagaData(Optional.of(this.studentTwinID));
    this.sagaData = this.getPenRequestBatchUnmatchSagaDataFromJsonString(payload);
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
        UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));
  }

  /**
   * After.
   */
  @After
  public void after() {
    this.sagaEventRepository.deleteAll();
    this.repository.deleteAll();
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithTwins_shouldPostEventToPenMatchApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(this.sagaData))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(DELETE_POSSIBLE_MATCH);
    final List<Map<String, UUID>> payload = new ObjectMapper().readValue(newEvent.getEventPayload(), new TypeReference<>() {
    });
    assertThat(payload).size().isEqualTo(1);
    assertThat(payload.get(0)).containsKey("matchedStudentID").containsValue(UUID.fromString(this.studentTwinID));
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(DELETE_POSSIBLE_MATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testHandleEvent_givenEventAndSagaDataWithoutTwins_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    final var sagaFromDBtoUpdateOptional = this.sagaService.findSagaById(this.saga.getSagaId());
    if (sagaFromDBtoUpdateOptional.isPresent()) {
      final var sagaFromDBtoUpdate = sagaFromDBtoUpdateOptional.get();
      final var payload = JsonUtil.getJsonObjectFromString(PenRequestBatchUnmatchSagaData.class, sagaFromDBtoUpdate.getPayload());
      payload.setMatchedStudentIDList(null);
      sagaFromDBtoUpdate.setPayload(JsonUtil.getJsonStringFromObject(payload));
      this.sagaService.updateAttachedEntityDuringSagaProcess(sagaFromDBtoUpdate);
      this.saga = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    }
    final var payload = this.placeholderPenRequestBatchUnmatchSagaData(Optional.empty());
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(payload))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    assertThat(prbStudent.getStudentID()).isNull();
    assertThat(prbStudent.getAssignedPEN()).isNull();
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testUpdatePenRequestBatchStudent_givenEventAndSagaData_shouldPostEventToBatchApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var studentTwinIDs = List.of(this.studentTwinID);
    final var event = Event.builder()
        .eventType(DELETE_POSSIBLE_MATCH)
        .eventOutcome(EventOutcome.POSSIBLE_MATCH_DELETED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentTwinIDs))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_REQUEST_BATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT);
    final var prbStudent = new ObjectMapper().readValue(newEvent.getEventPayload(), PenRequestBatchStudent.class);
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    assertThat(prbStudent.getStudentID()).isNull();
    assertThat(prbStudent.getAssignedPEN()).isNull();
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(DELETE_POSSIBLE_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.POSSIBLE_MATCH_DELETED.toString());
  }

  /**
   * Dummy pen request batch unmatch saga data json string.
   *
   * @return the string
   */
  protected String placeholderPenRequestBatchUnmatchSagaData(final Optional<String> studentTwinID) {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"studentID\": \"" + UUID.randomUUID().toString() + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \"" + this.mincode + "\",\n" +
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
  protected PenRequestBatchUnmatchSagaData getPenRequestBatchUnmatchSagaDataFromJsonString(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestBatchUnmatchSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
