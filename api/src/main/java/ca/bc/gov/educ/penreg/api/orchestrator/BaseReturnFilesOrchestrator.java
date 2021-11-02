package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.properties.PenCoordinatorProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.*;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueTypeCode;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.*;
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
  private final PenCoordinatorService penCoordinatorService;
  @Getter(PROTECTED)
  private final ResponseFileGeneratorService responseFileGeneratorService;
  @Getter(PROTECTED)
  private final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService;
  @Getter(PROTECTED)
  private final RestUtils restUtils;
  @Getter(PROTECTED)
  private final PenCoordinatorProperties penCoordinatorProperties;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService              the saga service
   * @param messagePublisher         the message publisher
   * @param clazz                    the clazz
   * @param sagaName                 the saga name
   * @param topicToSubscribe         the topic to subscribe
   * @param penRequestBatchService   the pen request batch service
   * @param penCoordinatorService    the pen coordinator service
   * @param penCoordinatorProperties the pen coordinator properties
   */
  protected BaseReturnFilesOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher,
                                        final Class<T> clazz, final String sagaName, final String topicToSubscribe,
                                        final PenRequestBatchService penRequestBatchService,
                                        final PenCoordinatorService penCoordinatorService,
                                        final PenCoordinatorProperties penCoordinatorProperties,
                                        final ResponseFileGeneratorService responseFileGeneratorService,
                                        final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService,
                                        final RestUtils restUtils) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
    this.penRequestBatchService = penRequestBatchService;
    this.penCoordinatorService = penCoordinatorService;
    this.penCoordinatorProperties = penCoordinatorProperties;
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
      penRequestBatchReturnFilesSagaData.setPenCoordinator(this.getPenCoordinator(penRequestBatch.get()));
      penRequestBatchReturnFilesSagaData.setFromEmail(this.penCoordinatorProperties.getFromEmail());
      penRequestBatchReturnFilesSagaData.setTelephone(this.penCoordinatorProperties.getTelephone());
      penRequestBatchReturnFilesSagaData.setFacsimile(this.penCoordinatorProperties.getFacsimile());
      penRequestBatchReturnFilesSagaData.setMailingAddress(this.penCoordinatorProperties.getMailingAddress());
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

    final List<String> studentIDs = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents() == null ? new ArrayList<>() : penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream()
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
      .fromEmail(this.getPenCoordinatorProperties().getFromEmail())
      .mincode(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode())
      .submissionNumber(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getSubmissionNumber())
      .schoolName(penRequestBatchReturnFilesSagaData.getSchoolName())
      .pendingRecords(pendingRecords)
      .build();
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .replyTo(this.getTopicToSubscribe())
      .build();

    //set toEmail and email type depending on whether a penCoordinator exists for the mincode
    if (eventType.equals(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT)) {
      nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
      penRequestBatchArchivedEmailEvent.setToEmail(this.getPenCoordinatorProperties().getFromEmail());
    } else {
      nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
      penRequestBatchArchivedEmailEvent.setToEmail(penRequestBatchReturnFilesSagaData.getPenCoordinator().getPenCoordinatorEmail());
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


  protected boolean hasPenCoordinatorEmail(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return !this.hasNoPenCoordinatorEmail(penRequestBatchReturnFilesSagaData);
  }

  protected boolean hasNoPenCoordinatorEmail(final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return penRequestBatchReturnFilesSagaData.getPenCoordinator() == null ||
      penRequestBatchReturnFilesSagaData.getPenCoordinator().getPenCoordinatorEmail() == null ||
      penRequestBatchReturnFilesSagaData.getPenCoordinator().getPenCoordinatorEmail().isEmpty();
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

  protected void sendHasNoCoordinatorEmail(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
  }

  protected void sendHasCoordinatorEmail(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
  }

  protected PenCoordinator getPenCoordinator(final PenRequestBatchEntity penRequestBatchEntity) {
    try {
      final var penCoordinatorOptional = this.getPenCoordinatorService().getPenCoordinatorByMinCode(penRequestBatchEntity.getMincode());
      return penCoordinatorOptional.orElse(null);
    } catch (final NullPointerException e) {
      log.error("Error while trying to get get pen coordinator. The pen coordinator map is null", e);
      return null;
    }
  }

  protected void logStudentsNotFound(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    log.error("Student record(s) were not found. This should not happen. Please check the student api. :: {}", saga.getSagaId());
  }

  protected void logPenRequestBatchNotFound(final Event event, final Saga saga, final BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    log.error("Pen request batch record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
  }
}
