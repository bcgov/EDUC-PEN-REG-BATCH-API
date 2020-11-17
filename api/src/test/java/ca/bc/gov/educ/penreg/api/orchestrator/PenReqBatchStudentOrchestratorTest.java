package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchTypeCode;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
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
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.PROCESS_PEN_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.VALIDATE_STUDENT_DEMOGRAPHICS;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_SERVICES_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenReqBatchStudentOrchestratorTest extends BaseOrchestratorTest {
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
  private PenReqBatchStudentOrchestrator orchestrator;

  /**
   * The Saga.
   */
  private Saga saga;
  /**
   * The Saga data.
   */
  private PenRequestBatchStudentSagaData sagaData;
  String studentID = UUID.randomUUID().toString();
  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<Student> createStudentCaptor;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  private PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  RestTemplate restTemplate;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    PenRequestBatchStudentEntity penRequestBatchStudentEntity = new PenRequestBatchStudentEntity();
    penRequestBatchStudentEntity.setPenRequestBatchStudentStatusCode(LOADED.getCode());
    penRequestBatchStudentEntity.setCreateDate(LocalDateTime.now());
    penRequestBatchStudentEntity.setUpdateDate(LocalDateTime.now());
    penRequestBatchStudentEntity.setCreateUser("TEST");
    penRequestBatchStudentEntity.setUpdateUser("TEST");
    penRequestBatchStudentEntity.setAssignedPEN(TEST_PEN);
    penRequestBatchStudentEntity.setDob("19650101");
    penRequestBatchStudentEntity.setGenderCode("M");
    penRequestBatchStudentEntity.setLocalID("20345678");
    penRequestBatchStudentEntity.setGradeCode("01");
    PenRequestBatchEntity entity = new PenRequestBatchEntity();
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser("TEST");
    entity.setUpdateUser("TEST");
    entity.setPenRequestBatchStatusCode(LOADED.getCode());
    entity.setSubmissionNumber("12345678");
    entity.setPenRequestBatchTypeCode(PenRequestBatchTypeCode.SCHOOL.getCode());
    entity.setSchoolGroupCode("K12");
    entity.setUnarchivedBatchStatusCode("N");
    entity.setUnarchivedUser("TEST");
    entity.setUnarchivedBatchChangedFlag("N");
    entity.setFileName("test");
    entity.setFileType("PEN");
    entity.setMinCode("10200000");
    entity.setMinistryPRBSourceCode("PEN_WEB");
    entity.setInsertDate(LocalDateTime.now());
    entity.setExtractDate(LocalDateTime.now());
    entity.setSourceStudentCount(1L);
    entity.setStudentCount(1L);
    entity.setSourceApplication("PEN");
    penRequestBatchStudentEntity.setPenRequestBatchEntity(entity);
    entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
    penRequestBatchRepository.save(entity);

    var payload = placeholderPenRequestBatchActionsSagaData();
    sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, payload);
    sagaData.setDob("19650101");
    sagaData.setAssignedPEN(TEST_PEN);
    sagaData.setLocalID("20345678");
    sagaData.setGradeCode("01");
    sagaData.setStudentID(studentID);
    sagaData.setPenRequestBatchID(entity.getPenRequestBatchID());
    sagaData.setPenRequestBatchStudentID(entity.getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentID());
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(sagaData),
        UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));

  }

  @After
  public void tearDown() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
    penRequestBatchRepository.deleteAll();
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenType_shouldExecuteNextEventVALIDATE_STUDENT_DEMOGRAPHICS() throws InterruptedException, TimeoutException, IOException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(EventType.INITIATED)
                     .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_SERVICES_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(VALIDATE_STUDENT_DEMOGRAPHICS);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(saga.getSagaId().toString());
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithoutErrorWarning_shouldExecuteNextEventPROCESS_PEN_MATCH() throws InterruptedException, TimeoutException, IOException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
                     .eventOutcome(EventOutcome.VALIDATION_SUCCESS_NO_ERROR_WARNING)
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(sagaData.getDob());
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_NO_ERROR_WARNING.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithWarning_shouldExecuteNextEventPROCESS_PEN_MATCH() throws InterruptedException, TimeoutException, IOException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
                     .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
                     .eventOutcome(EventOutcome.VALIDATION_SUCCESS_WITH_ONLY_WARNING)
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(sagaData.getDob());
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_WITH_ONLY_WARNING.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithError_shouldExecuteNextEventMARK_SAGA_COMPLETE() throws InterruptedException, TimeoutException, IOException {
    var event = Event.builder()
                     .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
                     .eventOutcome(EventOutcome.VALIDATION_SUCCESS_WITH_ERROR)
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_WITH_ERROR.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultD0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    runCreateNewStudentTestBasedOnArgument("D0");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultC0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    runCreateNewStudentTestBasedOnArgument("C0");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultB0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    runCreateNewStudentTestBasedOnArgument("B0");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultAA_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    runMatchStudentTestBasedOnArgument("AA");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultB1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    runMatchStudentTestBasedOnArgument("B1");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultC1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    runMatchStudentTestBasedOnArgument("C1");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultD1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    runMatchStudentTestBasedOnArgument("D1");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultBM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    runFixableStudentTestBasedOnArgument("BM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultCM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    runFixableStudentTestBasedOnArgument("CM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultDM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    runFixableStudentTestBasedOnArgument("DM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultG0_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    runFixableStudentTestBasedOnArgument("G0");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultF1_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    runFixableStudentTestBasedOnArgument("F1");
  }


  private void runFixableStudentTestBasedOnArgument(String penStatus) throws IOException, InterruptedException, TimeoutException {
    var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(new ArrayList<>());
    var event = Event.builder()
                     .eventType(PROCESS_PEN_MATCH)
                     .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
                     .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    var prb = penRequestBatchRepository.findAll().get(0);
    var prbStudent = penRequestBatchStudentRepository.findAllByPenRequestBatchEntity(prb).stream().findFirst().orElseThrow();
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }

  private void runMatchStudentTestBasedOnArgument(String penStatus) throws IOException, InterruptedException, TimeoutException {
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restUtils.getStudentByStudentID(studentID)).thenReturn(Student.builder().studentID(studentID).pen(TEST_PEN).build());
    doNothing().when(restUtils).updateStudent(createStudentCaptor.capture());
    List<PenMatchRecord> matchRecords = new ArrayList<>();
    PenMatchRecord record = new PenMatchRecord();
    record.setMatchingPEN(TEST_PEN);
    record.setStudentID(studentID);
    matchRecords.add(record);
    var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(matchRecords);
    var event = Event.builder()
                     .eventType(PROCESS_PEN_MATCH)
                     .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
                     .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    var studentPayload = createStudentCaptor.getValue();
    assertThat(studentPayload.getMincode()).isEqualTo("10200000");
    assertThat(studentPayload.getHistoryActivityCode()).isNotEmpty();
    assertThat(studentPayload.getHistoryActivityCode()).isEqualTo(REQ_MATCH.getCode());
    assertThat(studentPayload.getUpdateUser()).isEqualTo("ALGORITHM");
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }

  private void runCreateNewStudentTestBasedOnArgument(String penStatus) throws IOException, InterruptedException, TimeoutException {
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restUtils.getNextPenNumberFromPenServiceAPI(saga.getSagaId().toString())).thenReturn(TEST_PEN);
    when(restUtils.getStudentByPEN(TEST_PEN)).thenReturn(Optional.empty());
    when(restUtils.createStudent(createStudentCaptor.capture())).thenReturn(Student.builder().studentID(studentID).pen(TEST_PEN).build());

    var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(new ArrayList<>());
    var event = Event.builder()
                     .eventType(PROCESS_PEN_MATCH)
                     .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
                     .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
                     .sagaId(saga.getSagaId())
                     .build();
    orchestrator.handleEvent(event);
    var studentPayload = createStudentCaptor.getValue();
    assertThat(studentPayload.getDob()).isEqualTo("1965-01-01");
    assertThat(studentPayload.getHistoryActivityCode()).isNotEmpty();
    assertThat(studentPayload.getHistoryActivityCode()).isEqualTo(REQ_NEW.getCode());
    assertThat(studentPayload.getCreateUser()).isEqualTo("ALGORITHM");
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }
}
