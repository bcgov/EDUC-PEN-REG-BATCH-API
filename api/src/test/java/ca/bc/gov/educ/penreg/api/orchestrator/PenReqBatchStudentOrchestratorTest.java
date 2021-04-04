package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchTypeCode;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
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
    final PenRequestBatchStudentEntity penRequestBatchStudentEntity = new PenRequestBatchStudentEntity();
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
    final PenRequestBatchEntity entity = new PenRequestBatchEntity();
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser("TEST");
    entity.setUpdateUser("TEST");
    entity.setPenRequestBatchStatusCode(LOADED.getCode());
    entity.setSubmissionNumber("12345678");
    entity.setPenRequestBatchTypeCode(PenRequestBatchTypeCode.SCHOOL.getCode());
    entity.setSchoolGroupCode("K12");
    entity.setFileName("test");
    entity.setFileType("PEN");
    entity.setMincode("10200000");
    entity.setMinistryPRBSourceCode("PEN_WEB");
    entity.setInsertDate(LocalDateTime.now());
    entity.setExtractDate(LocalDateTime.now());
    entity.setSourceStudentCount(1L);
    entity.setStudentCount(1L);
    entity.setSourceApplication("PEN");
    entity.setPenRequestBatchProcessTypeCode(PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode());
    penRequestBatchStudentEntity.setPenRequestBatchEntity(entity);
    entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
    this.penRequestBatchRepository.save(entity);

    final var payload = this.placeholderPenRequestBatchActionsSagaData();
    this.sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, payload);
    this.sagaData.setDob("19650101");
    this.sagaData.setAssignedPEN(TEST_PEN);
    this.sagaData.setLocalID("20345678");
    this.sagaData.setGradeCode("01");
    this.sagaData.setStudentID(this.studentID);
    this.sagaData.setPenRequestBatchID(entity.getPenRequestBatchID());
    this.sagaData.setPenRequestBatchStudentID(entity.getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentID());
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
        UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));

  }


  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenType_shouldExecuteNextEventVALIDATE_STUDENT_DEMOGRAPHICS() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_SERVICES_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(VALIDATE_STUDENT_DEMOGRAPHICS);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(this.saga.getSagaId().toString());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithoutErrorWarning_shouldExecuteNextEventPROCESS_PEN_MATCH() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
        .eventOutcome(EventOutcome.VALIDATION_SUCCESS_NO_ERROR_WARNING)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(this.sagaData.getDob());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_NO_ERROR_WARNING.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithWarning_shouldExecuteNextEventPROCESS_PEN_MATCH() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    final var event = Event.builder()
        .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
        .eventOutcome(EventOutcome.VALIDATION_SUCCESS_WITH_ONLY_WARNING)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains(this.sagaData.getDob());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_WITH_ONLY_WARNING.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndValidationWithError_shouldExecuteNextEventMARK_SAGA_COMPLETE() throws InterruptedException, TimeoutException, IOException {
    final var event = Event.builder()
        .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
        .eventOutcome(EventOutcome.VALIDATION_SUCCESS_WITH_ERROR)
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_WITH_ERROR.toString());
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultD0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    this.runCreateNewStudentTestBasedOnArgument("D0");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultC0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    this.runCreateNewStudentTestBasedOnArgument("C0");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultB0_shouldMarkSagaCompleteAfterCreatingNewStudent() throws InterruptedException, TimeoutException, IOException {
    this.runCreateNewStudentTestBasedOnArgument("B0");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultAA_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    this.runMatchStudentTestBasedOnArgument("AA");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultB1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    this.runMatchStudentTestBasedOnArgument("B1");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultC1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    this.runMatchStudentTestBasedOnArgument("C1");
  }
  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultD1_shouldMarkSagaCompleteAfterUpdatingStudent() throws InterruptedException, TimeoutException, IOException {
    this.runMatchStudentTestBasedOnArgument("D1");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultBM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    this.runFixableStudentTestBasedOnArgument("BM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultCM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    this.runFixableStudentTestBasedOnArgument("CM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultDM_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    this.runFixableStudentTestBasedOnArgument("DM");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultG0_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    this.runFixableStudentTestBasedOnArgument("G0");
  }

  @Test
  public void testHandleEvent_givenValidSagaDataAndEvenTypeAndPenMatchResultF1_shouldMarkSagaComplete() throws InterruptedException, TimeoutException, IOException {
    this.runFixableStudentTestBasedOnArgument("F1");
  }


  private void runFixableStudentTestBasedOnArgument(final String penStatus) throws IOException, InterruptedException, TimeoutException {
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(new ArrayList<>());
    final var event = Event.builder()
        .eventType(PROCESS_PEN_MATCH)
        .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
        .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    final var prb = this.penRequestBatchRepository.findAll().get(0);
    final var prbStudent = this.penRequestBatchStudentRepository.findAllByPenRequestBatchEntity(prb).stream().findFirst().orElseThrow();
    assertThat(prbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(FIXABLE.getCode());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }

  private void runMatchStudentTestBasedOnArgument(final String penStatus) throws IOException, InterruptedException, TimeoutException {
    when(this.restUtils.getStudentByStudentID(this.studentID)).thenReturn(Student.builder().studentID(this.studentID).pen(TEST_PEN).build());
    doNothing().when(this.restUtils).updateStudent(this.createStudentCaptor.capture());
    final List<PenMatchRecord> matchRecords = new ArrayList<>();
    final PenMatchRecord record = new PenMatchRecord();
    record.setMatchingPEN(TEST_PEN);
    record.setStudentID(this.studentID);
    matchRecords.add(record);
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(matchRecords);
    final var event = Event.builder()
        .eventType(PROCESS_PEN_MATCH)
        .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
        .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    final var studentPayload = this.createStudentCaptor.getValue();
    assertThat(studentPayload.getMincode()).isEqualTo("10200000");
    assertThat(studentPayload.getHistoryActivityCode()).isNotEmpty();
    assertThat(studentPayload.getHistoryActivityCode()).isEqualTo(REQ_MATCH.getCode());
    assertThat(studentPayload.getUpdateUser()).isEqualTo("ALGORITHM");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }

  private void runCreateNewStudentTestBasedOnArgument(final String penStatus) throws IOException, InterruptedException, TimeoutException {
    when(this.restUtils.getNextPenNumberFromPenServiceAPI(this.saga.getSagaId().toString())).thenReturn(TEST_PEN);
    when(this.restUtils.getStudentByPEN(TEST_PEN)).thenReturn(Optional.empty());
    when(this.restUtils.createStudent(this.createStudentCaptor.capture())).thenReturn(Student.builder().studentID(this.studentID).pen(TEST_PEN).build());

    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(new ArrayList<>());
    final var event = Event.builder()
        .eventType(PROCESS_PEN_MATCH)
        .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
        .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.handleEvent(event);
    final var studentPayload = this.createStudentCaptor.getValue();
    assertThat(studentPayload.getDob()).isEqualTo("1965-01-01");
    assertThat(studentPayload.getHistoryActivityCode()).isNotEmpty();
    assertThat(studentPayload.getHistoryActivityCode()).isEqualTo(REQ_NEW.getCode());
    assertThat(studentPayload.getCreateUser()).isEqualTo("ALGORITHM");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_PROCESSED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.PROCESS_PEN_MATCH_RESULTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_MATCH_RESULTS_PROCESSED.toString());
  }
}
