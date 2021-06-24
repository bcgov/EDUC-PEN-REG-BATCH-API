package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
public abstract class BaseReturnFilesOrchestrator<T> extends BaseOrchestrator<T> {

  protected static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  protected static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;
  protected static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
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
   * The constant PEN_REQUEST_BATCH_API.
   */
  protected String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

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
  public BaseReturnFilesOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                     Class<T> clazz, String sagaName, String topicToSubscribe,
                                     PenRequestBatchService penRequestBatchService,
                                     PenCoordinatorService penCoordinatorService,
                                     PenCoordinatorProperties penCoordinatorProperties,
                                     ResponseFileGeneratorService responseFileGeneratorService,
                                     PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService,
                                     RestUtils restUtils) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
    this.penRequestBatchService = penRequestBatchService;
    this.penCoordinatorService = penCoordinatorService;
    this.penCoordinatorProperties = penCoordinatorProperties;
    this.responseFileGeneratorService = responseFileGeneratorService;
    this.penRequestBatchStudentValidationIssueService = penRequestBatchStudentValidationIssueService;
    this.restUtils = restUtils;
  }

  protected void gatherReportData(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GATHER_REPORT_DATA.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    var penRequestBatch = getPenRequestBatchService().findById(penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
    Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(GATHER_REPORT_DATA).replyTo(this.getTopicToSubscribe()).build();

    if (penRequestBatch.isPresent()) {
      penRequestBatchReturnFilesSagaData.setPenRequestBatchStudentValidationIssues(getValidationIssues(penRequestBatch.get()));
      List<PenRequestBatchStudent> studentRequests = penRequestBatch.get().getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList());
      penRequestBatchReturnFilesSagaData.setPenRequestBatchStudents(studentRequests);
      penRequestBatchReturnFilesSagaData.setPenRequestBatch(mapper.toStructure(penRequestBatch.get()));
      penRequestBatchReturnFilesSagaData.setPenCoordinator(this.getPenCoordinator(penRequestBatch.get()));
      penRequestBatchReturnFilesSagaData.setFromEmail(penCoordinatorProperties.getFromEmail());
      penRequestBatchReturnFilesSagaData.setTelephone(penCoordinatorProperties.getTelephone());
      penRequestBatchReturnFilesSagaData.setFacsimile(penCoordinatorProperties.getFacsimile());
      penRequestBatchReturnFilesSagaData.setMailingAddress(penCoordinatorProperties.getMailingAddress());
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchReturnFilesSagaData)); // save the updated payload to DB...
      this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
      nextEvent.setEventOutcome(REPORT_DATA_GATHERED);
    } else {
      log.error("PenRequestBatch not found during archive and return saga. This is not expected :: {}", penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
      nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
    }
    this.handleEvent(nextEvent);
  }

  private HashMap<String, String> getValidationIssues(PenRequestBatchEntity penRequestBatch) {
    var validationIssues = penRequestBatchStudentValidationIssueService.findAllPenRequestBatchStudentValidationIssueEntities(penRequestBatch);
    var validationIssuesByPrbStudentID = new HashMap<String, String>();
    validationIssues.forEach(issue -> {
      if (issue.getPenRequestBatchValidationIssueSeverityCode().equals(PenRequestBatchStudentValidationIssueSeverityCode.ERROR.toString())) {
        var prbStudentID = issue.getPenRequestBatchStudentEntity().getPenRequestBatchStudentID().toString();
        var errorDescription = restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode(issue.getPenRequestBatchValidationIssueTypeCode())
          .map(PenRequestBatchStudentValidationIssueTypeCode::getDescription)
          .orElse(issue.getPenRequestBatchValidationIssueTypeCode());
        if (validationIssuesByPrbStudentID.containsKey(prbStudentID)) {
          errorDescription = validationIssuesByPrbStudentID.get(prbStudentID) + ". " + errorDescription;
        }
        validationIssuesByPrbStudentID.put(prbStudentID, errorDescription);
      }
    });
    return validationIssuesByPrbStudentID;
  }

  protected void getStudents(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, TimeoutException, InterruptedException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_STUDENTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.GET_STUDENTS).replyTo(this.getTopicToSubscribe()).build();

    List<String> studentIDs = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream()
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

  protected void saveReports(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(SAVE_REPORTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    getResponseFileGeneratorService().saveReports(event.getEventPayload(),
      mapper.toModel(penRequestBatchReturnFilesSagaData.getPenRequestBatch()),
      penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents(),
      penRequestBatchReturnFilesSagaData.getStudents(),
      reportMapper.toReportData(penRequestBatchReturnFilesSagaData));

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(SAVE_REPORTS)
      .eventOutcome(REPORTS_SAVED)
      .build();
    this.handleEvent(nextEvent);
  }

  protected void sendArchivedEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData, EventType eventType) throws JsonProcessingException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(eventType.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val pendingCount = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream().filter(this::getPenRequestBatchStudentPendingCountPredicate).count();
    final PendingRecords pendingRecords;
    if (pendingCount == (long) penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().size()) {
      pendingRecords = PendingRecords.ALL;
    } else if (pendingCount > 0) {
      pendingRecords = PendingRecords.SOME;
    } else {
      pendingRecords = PendingRecords.NONE;
    }
    PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
      .fromEmail(this.getPenCoordinatorProperties().getFromEmail())
      .mincode(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode())
      .submissionNumber(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getSubmissionNumber())
      .schoolName(penRequestBatchReturnFilesSagaData.getSchoolName())
      .pendingRecords(pendingRecords)
      .build();
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
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

  private boolean getPenRequestBatchStudentPendingCountPredicate(PenRequestBatchStudent el) {
    return el.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.INFOREQ.getCode())
      || el.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.FIXABLE.getCode())
      || el.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode());
  }


  protected boolean hasPenCoordinatorEmail(BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return !this.hasNoPenCoordinatorEmail(penRequestBatchReturnFilesSagaData);
  }

  protected boolean hasNoPenCoordinatorEmail(BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
    return penRequestBatchReturnFilesSagaData.getPenCoordinator() == null ||
      penRequestBatchReturnFilesSagaData.getPenCoordinator().getPenCoordinatorEmail() == null ||
      penRequestBatchReturnFilesSagaData.getPenCoordinator().getPenCoordinatorEmail().isEmpty();
  }

  protected void sendHasNoCoordinatorEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
  }

  protected void sendHasCoordinatorEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
    this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
  }

  protected PenCoordinator getPenCoordinator(PenRequestBatchEntity penRequestBatchEntity) {
    try {
      var penCoordinatorOptional = getPenCoordinatorService().getPenCoordinatorByMinCode(penRequestBatchEntity.getMincode());
      return penCoordinatorOptional.orElse(null);
    } catch (NullPointerException e) {
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
