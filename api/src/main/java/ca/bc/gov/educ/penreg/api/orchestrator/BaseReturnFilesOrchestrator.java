package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.properties.DataManagementUnitProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.*;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueTypeCode;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
public abstract class BaseReturnFilesOrchestrator<T> extends BaseOrchestrator<T> {

  protected static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  protected static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;
  protected static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  protected static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
  protected final ObjectMapper obMapper = new ObjectMapper();
  /**
   * The Pen request batch service.
   */
  @Getter(PROTECTED)
  private final PenRequestBatchService penRequestBatchService;
  @Getter(PROTECTED)
  private final StudentRegistrationContactService studentRegistrationContactService;
  @Getter(PROTECTED)
  private final ResponseFileGeneratorService responseFileGeneratorService;
  @Getter(PROTECTED)
  private final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService;
  @Getter(PROTECTED)
  private final RestUtils restUtils;
  @Getter(PROTECTED)
  private final DataManagementUnitProperties dataManagementUnitProperties;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService              the saga service
   * @param messagePublisher         the message publisher
   * @param clazz                    the clazz
   * @param sagaName                 the saga name
   * @param topicToSubscribe         the topic to subscribe
   * @param penRequestBatchService   the pen request batch service
   * @param studentRegistrationContactService    the student registration contact service
   * @param dataManagementUnitProperties the data mangement unit properties
   */
  protected BaseReturnFilesOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher,
                                        final Class<T> clazz, final String sagaName, final String topicToSubscribe,
                                        final PenRequestBatchService penRequestBatchService,
                                        final StudentRegistrationContactService studentRegistrationContactService,
                                        final DataManagementUnitProperties dataManagementUnitProperties,
                                        final ResponseFileGeneratorService responseFileGeneratorService,
                                        final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService,
                                        final RestUtils restUtils) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
    this.penRequestBatchService = penRequestBatchService;
    this.studentRegistrationContactService = studentRegistrationContactService;
    this.dataManagementUnitProperties = dataManagementUnitProperties;
    this.responseFileGeneratorService = responseFileGeneratorService;
    this.penRequestBatchStudentValidationIssueService = penRequestBatchStudentValidationIssueService;
    this.restUtils = restUtils;
  }

  protected void gatherReportData(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GATHER_REPORT_DATA.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final var penRequestBatch = this.getPenRequestBatchService().findById(penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
    val nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(GATHER_REPORT_DATA).replyTo(this.getTopicToSubscribe()).build();

    if (penRequestBatch.isPresent()) {
      penRequestBatchReturnFilesSagaData.setPenRequestBatchStudentValidationIssues(this.getValidationIssues(penRequestBatch.get()));
      final List<PenRequestBatchStudent> studentRequests = penRequestBatch.get().getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList());
      penRequestBatchReturnFilesSagaData.setPenRequestBatchStudents(studentRequests);
      penRequestBatchReturnFilesSagaData.setPenRequestBatch(mapper.toStructure(penRequestBatch.get()));
      penRequestBatchReturnFilesSagaData.setStudentRegistrationContacts(this.getStudentRegistrationContactsByMincode(penRequestBatch.get()));
      penRequestBatchReturnFilesSagaData.setFromEmail(this.dataManagementUnitProperties.getFromEmail());
      penRequestBatchReturnFilesSagaData.setTelephone(this.dataManagementUnitProperties.getTelephone());
      penRequestBatchReturnFilesSagaData.setFacsimile(this.dataManagementUnitProperties.getFacsimile());
      penRequestBatchReturnFilesSagaData.setMailingAddress(this.dataManagementUnitProperties.getMailingAddress());
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchReturnFilesSagaData)); // save the updated payload to DB...
      this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
      nextEvent.setEventOutcome(REPORT_DATA_GATHERED);
    } else {
      log.error("PenRequestBatch not found during archive and return saga. This is not expected :: {}", penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
      nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
    }
    this.handleEvent(nextEvent);
  }

  private HashMap<String, String> getValidationIssues(final PenRequestBatchEntity penRequestBatch) {
    final var validationIssues = this.penRequestBatchStudentValidationIssueService.findAllPenRequestBatchStudentValidationIssueEntities(penRequestBatch);
    final var validationIssuesByPrbStudentID = new HashMap<String, String>();
    validationIssues.forEach(issue -> {
      if (issue.getPenRequestBatchValidationIssueSeverityCode().equals(PenRequestBatchStudentValidationIssueSeverityCode.ERROR.toString())) {
        final var prbStudentID = issue.getPenRequestBatchStudentEntity().getPenRequestBatchStudentID().toString();
        var errorDescription = this.restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode(issue.getPenRequestBatchValidationIssueTypeCode())
          .map(PenRequestBatchStudentValidationIssueTypeCode::getDescription)
          .orElse(issue.getPenRequestBatchValidationIssueTypeCode());
        var fieldDescription = this.restUtils.getPenRequestBatchStudentValidationIssueFieldCodeInfoByIssueFieldCode(issue.getPenRequestBatchValidationFieldCode())
          .map(PenRequestBatchStudentValidationIssueFieldCode::getDescription)
          .orElse(issue.getPenRequestBatchValidationFieldCode());

        var fullError = fieldDescription + " - " + errorDescription;

        if (validationIssuesByPrbStudentID.containsKey(prbStudentID)) {
          fullError = validationIssuesByPrbStudentID.get(prbStudentID) + ". " + fullError;
        }
        validationIssuesByPrbStudentID.put(prbStudentID, fullError);
      }
    });
    return validationIssuesByPrbStudentID;
  }

  protected void getStudents(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, TimeoutException, InterruptedException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_STUDENTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    val nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.GET_STUDENTS).replyTo(this.getTopicToSubscribe()).build();
    if (penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents() == null) {
      throw new PenRegAPIRuntimeException("penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents() is null which is not expected in this flow for batch id :: " + penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
    }
    final List<String> studentIDs = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream()
      .map(PenRequestBatchStudent::getStudentID).filter(Objects::nonNull).collect(Collectors.toList());

    if (studentIDs.isEmpty()) {
      nextEvent.setEventOutcome(STUDENTS_FOUND);
      nextEvent.setEventPayload("[]");
      this.handleEvent(nextEvent);
    } else {
      nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(studentIDs));
      this.postMessageToTopic(SagaTopicsEnum.STUDENT_API_TOPIC.toString(), nextEvent);
      log.info("message sent to STUDENT_API_TOPIC for {} Event. :: {}", GET_STUDENTS, saga.getSagaId());
    }
  }

  protected void saveReports(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(SAVE_REPORTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    if (penRequestBatchReturnFilesSagaData.getStudents() == null) {
      log.info("students in saga data is null or empty for batch id :: {} and saga id :: {}, setting it from event states table", penRequestBatchReturnFilesSagaData.getPenRequestBatchID(), saga.getSagaId());
      SagaEvent sagaEvent = SagaEvent.builder().sagaEventState(GET_STUDENTS.toString()).sagaEventOutcome(STUDENTS_FOUND.toString()).sagaStepNumber(3).build();
      val sagaEventOptional = this.getSagaService().findSagaEvent(saga, sagaEvent);
      if (sagaEventOptional.isPresent()) {
        List<Student> students = obMapper.readValue(sagaEventOptional.get().getSagaEventResponse(), new TypeReference<>() {
        });
        penRequestBatchReturnFilesSagaData.setStudents(event, students);
      } else {
        throw new PenRegAPIRuntimeException("students not found in event states table for saga id :: " + saga.getSagaId());
      }
    }
    this.getResponseFileGeneratorService().saveReports(event.getEventPayload(),
      mapper.toModel(penRequestBatchReturnFilesSagaData.getPenRequestBatch()),
      penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents(),
      penRequestBatchReturnFilesSagaData.getStudents(),
      reportMapper.toReportData(penRequestBatchReturnFilesSagaData));

    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(SAVE_REPORTS)
      .eventOutcome(REPORTS_SAVED)
      .build();
    this.handleEvent(nextEvent);
  }

  protected void sendArchivedEmail(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData, final EventType eventType) throws JsonProcessingException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(eventType.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val prbStudentStatusCodeMap = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents() == null ? new HashMap<String, Long>() : penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream().collect(groupingBy(PenRequestBatchStudent::getPenRequestBatchStudentStatusCode, counting()));
    log.debug("PRBStudent status code map :: {}", prbStudentStatusCodeMap);
    final PendingRecords pendingRecords;
    // if all PRBStudent records have ERROR status then all of them are pending.
    if (this.getValueFromMap(ERROR.getCode(), prbStudentStatusCodeMap) > 0
      && (this.getValueFromMap(ERROR.getCode(), prbStudentStatusCodeMap) == penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().size())) {
      pendingRecords = PendingRecords.ALL;
    } else if (this.areSomeRecordsPending(prbStudentStatusCodeMap)) {
      pendingRecords = PendingRecords.SOME;
    } else {
      pendingRecords = PendingRecords.NONE;
    }
    log.debug("Pending Records value :: {}", pendingRecords);
    final PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
      .fromEmail(this.getDataManagementUnitProperties().getFromEmail())
      .mincode(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode())
      .submissionNumber(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getSubmissionNumber())
      .schoolName(penRequestBatchReturnFilesSagaData.getSchoolName())
      .pendingRecords(pendingRecords)
      .build();
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .replyTo(this.getTopicToSubscribe())
      .build();

    //set toEmail and email type depending on whether a student registration school contact exists for the mincode
    if (eventType.equals(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT)) {
      nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
      penRequestBatchArchivedEmailEvent.setToEmail(new ArrayList<>(List.of(this.getDataManagementUnitProperties().getFromEmail())));
    } else {
      nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
      penRequestBatchArchivedEmailEvent.setToEmail(penRequestBatchReturnFilesSagaData.getStudentRegistrationContacts().stream().map(SchoolContact::getEmail).toList());
    }
    nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent));

    this.postMessageToTopic(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for {} Event. :: {}", eventType, saga.getSagaId());
  }

  private boolean areSomeRecordsPending(final Map<String, Long> prbStudentStatusCodeMap) {
    val errorCount = this.getValueFromMap(ERROR.getCode(), prbStudentStatusCodeMap);
    val reqInfoCount = this.getValueFromMap(INFOREQ.getCode(), prbStudentStatusCodeMap);
    val fixableCount = this.getValueFromMap(FIXABLE.getCode(), prbStudentStatusCodeMap);
    return errorCount > 0 || reqInfoCount > 0 || fixableCount > 0;
  }

  private long getValueFromMap(final String code, final Map<String, Long> prbStudentStatusCodeMap) {
    val count = prbStudentStatusCodeMap.get(code);
    return count == null ? 0 : count;
  }


  protected boolean hasStudentRegistrationContactEmail(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return !this.hasNoStudentRegistrationContactEmail(penRequestBatchReturnFilesSagaData);
  }

  protected boolean hasNoStudentRegistrationContactEmail(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return penRequestBatchReturnFilesSagaData.getStudentRegistrationContacts().isEmpty();
  }

  protected boolean isSupportingPDFGeneration(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return !this.isNotSupportingPDFGeneration(penRequestBatchReturnFilesSagaData);
  }

  protected boolean isNotSupportingPDFGeneration(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    if (penRequestBatchReturnFilesSagaData.getPenRequestBatch() == null || penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode() == null) {
      return true;
    }
    return PenRegBatchHelper.getSchoolTypeCodeFromMincode(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode()) == SchoolTypeCode.SFAS ||
      (penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents() != null
        && !penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().isEmpty()
        && penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().size() > this.restUtils.getProps().getBlockPdfGenerationThreshold());
  }

  protected void sendHasNoStudentRegistrationContactEmail(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
  }

  protected void sendHasStudentRegistrationContactEmail(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
  }

  protected List<SchoolContact> getStudentRegistrationContactsByMincode(final PenRequestBatchEntity penRequestBatchEntity) {
    try {
      return this.getStudentRegistrationContactService().getStudentRegistrationContactsByMincode(penRequestBatchEntity.getMincode());
    } catch (final NullPointerException e) {
      log.error("Error while trying to get get student registration contact. The student registration contact map is null", e);
      return Collections.emptyList();
    }
  }

  protected void logStudentsNotFound(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    log.error("Student record(s) were not found. This should not happen. Please check the student api. :: {}", saga.getSagaId());
  }

  protected void logPenRequestBatchNotFound(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    log.error("Pen request batch record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
  }
}
