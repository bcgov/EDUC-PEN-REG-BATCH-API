package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.struct.v1.StudentHistory;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.USER_NEW;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
   * the restUtils
   */
  @Autowired
  RestUtils restUtils;

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
    final List<PossibleMatch> payload = new ObjectMapper().readValue(newEvent.getEventPayload(), new TypeReference<>() {
    });
    assertThat(payload).size().isEqualTo(1);
    assertThat(payload.get(0).getMatchedStudentID()).isEqualTo(this.studentTwinID);
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

  @Test
  public void testReadAuditHistory_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT)
        .eventOutcome(EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(this.sagaData))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT_HISTORY);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(GET_STUDENT_HISTORY.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED.toString());
  }

  @Test
  public void testRevertStudentInformation_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    when(this.restUtils.getStudentByStudentID(studentID)).thenReturn(new Student());

    List<StudentHistory> studentAuditHistory = getStudentAuditHistoryForRevertStudentInformationTest();
    final var event = Event.builder()
        .eventType(GET_STUDENT_HISTORY)
        .eventOutcome(EventOutcome.STUDENT_HISTORY_FOUND)
        .sagaId(this.saga.getSagaId())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentAuditHistory))
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final var currentSaga = sagaFromDB.get();
    assertThat(currentSaga.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_STUDENT_HISTORY.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_HISTORY_FOUND.toString());
    
    //checking that the necessary fields have been updated
    var studentUpdate = new ObjectMapper().readValue(newEvent.getEventPayload(), Student.class);
    assertThat(studentUpdate.getUsualFirstName()).isEqualTo("revert to this");
    assertThat(studentUpdate.getUsualMiddleNames()).isEqualTo("correct");
    assertThat(studentUpdate.getMincode()).isEqualTo(this.mincode);
    assertThat(studentUpdate.getLocalID()).isEqualTo("correct");
    assertThat(studentUpdate.getGradeCode()).isEqualTo("correct");
    assertThat(studentUpdate.getGradeYear()).isEqualTo("correct");
    assertThat(studentUpdate.getPostalCode()).isEqualTo("correct");
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
//        "    \"studentID\": \"" + UUID.randomUUID().toString() + "\",\n" +
        "    \"studentID\": \"" + this.studentID.toString() + "\",\n" +
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

  /**
   * returns a list of the student's audit history
   *
   * @return the student's audit history
   */
  private List<StudentHistory> getStudentAuditHistoryForRevertStudentInformationTest() {
    List<StudentHistory> studentAuditHistoryList = new ArrayList<>();

    studentAuditHistoryList.add(
        studentAuditHistoryCreatorForRevertStudentInformationTest("creation", "wrong", "wrong", "wrong","wrong","wrong","wrong", REQ_NEW.getCode(), "15-01-01"));
    studentAuditHistoryList.add(
        studentAuditHistoryCreatorForRevertStudentInformationTest("wrong", "wrong", "wrong", "wrong","wrong","wrong","wrong", REQ_MATCH.getCode(), "16-02-02"));
    studentAuditHistoryList.add(
        studentAuditHistoryCreatorForRevertStudentInformationTest("revert to this", "correct", "correct","correct","correct","correct","correct", USER_NEW.getCode(), "17-03-03"));
    studentAuditHistoryList.add(
        studentAuditHistoryCreatorForRevertStudentInformationTest("wrong", "wrong", "wrong","wrong","wrong","wrong","wrong", REQ_MATCH.getCode(), "18-04-04"));

    return studentAuditHistoryList;
  }

  /**
   * returns a list of the student's audit history
   *
   *@param usualFirstName usual first name
   *@param usualMiddleName usual middle name
   *@param usualLastName usual last name
   *@param historyActivityCode history activity code (i.e. REQ_MATCH)
   *@param createDate the create date
   *
   * @return the student's audit history
   */
  private StudentHistory studentAuditHistoryCreatorForRevertStudentInformationTest(final String usualFirstName, final String usualMiddleName, final String usualLastName, final String localID, final String gradeCode, final String gradeYear,  final String postalCode, final String historyActivityCode, final String createDate) {
    return StudentHistory.builder()
        .usualFirstName(usualFirstName)
        .usualMiddleNames(usualMiddleName)
        .usualLastName(usualLastName)
        .mincode(this.mincode)
        .localID(localID)
        .gradeCode(gradeCode)
        .gradeYear(gradeYear)
        .postalCode(postalCode)
        .historyActivityCode(historyActivityCode)
        .createDate(createDate)
        .build();
  }
}
