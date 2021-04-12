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
import ca.bc.gov.educ.penreg.api.service.PenCoordinatorService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.ResponseFileGeneratorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.BasePenRequestBatchReturnFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchivedEmailEvent;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
public abstract class BaseReturnFilesOrchestrator<T> extends BaseOrchestrator<T> {

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
    private final PenCoordinatorProperties penCoordinatorProperties;

    protected static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
    protected static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;

    protected static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;

    protected final ObjectMapper obMapper = new ObjectMapper();

    /**
     * The constant PEN_REQUEST_BATCH_API.
     */
    protected String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

    /**
     * Instantiates a new Base orchestrator.
     *
     * @param sagaService      the saga service
     * @param messagePublisher the message publisher
     * @param clazz            the clazz
     * @param sagaName         the saga name
     * @param topicToSubscribe the topic to subscribe
     * @param penRequestBatchService the pen request batch service
     * @param penCoordinatorService  the pen coordinator service
     * @param penCoordinatorProperties the pen coordinator properties
     */
    public BaseReturnFilesOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                       Class<T> clazz, String sagaName, String topicToSubscribe,
                                       PenRequestBatchService penRequestBatchService,
                                       PenCoordinatorService penCoordinatorService,
                                       PenCoordinatorProperties penCoordinatorProperties,
                                       ResponseFileGeneratorService responseFileGeneratorService) {
        super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
        this.penRequestBatchService = penRequestBatchService;
        this.penCoordinatorService = penCoordinatorService;
        this.penCoordinatorProperties = penCoordinatorProperties;
        this.responseFileGeneratorService = responseFileGeneratorService;
    }

    protected void gatherReportData(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GATHER_REPORT_DATA.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var penRequestBatch = getPenRequestBatchService().findById(penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(GATHER_REPORT_DATA).replyTo(this.getTopicToSubscribe()).build();

        if(penRequestBatch.isPresent()) {
            List<PenRequestBatchStudent> studentRequests = penRequestBatch.get().getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList());
            penRequestBatchReturnFilesSagaData.setPenRequestBatchStudents(studentRequests);
            penRequestBatchReturnFilesSagaData.setPenCordinatorEmail(this.getPenCoordinatorEmail(penRequestBatch.get()));
            penRequestBatchReturnFilesSagaData.setFromEmail(penCoordinatorProperties.getFromEmail());
            penRequestBatchReturnFilesSagaData.setTelephone(penCoordinatorProperties.getTelephone());
            penRequestBatchReturnFilesSagaData.setFacsimile(penCoordinatorProperties.getFacsimile());
            penRequestBatchReturnFilesSagaData.setMailingAddress(penCoordinatorProperties.getMailingAddress());
            saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchReturnFilesSagaData)); // save the updated payload to DB...
            nextEvent.setEventOutcome(REPORT_DATA_GATHERED);
        } else {
            log.error("PenRequestBatch not found during archive and return saga. This is not expected :: {}", penRequestBatchReturnFilesSagaData.getPenRequestBatchID());
            nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
        }
        this.handleEvent(nextEvent);
    }

    protected void getStudents(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, TimeoutException, InterruptedException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GET_STUDENTS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.GET_STUDENTS).replyTo(this.getTopicToSubscribe()).build();

        List<String> studentIDs = penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents().stream()
          .map(PenRequestBatchStudent::getStudentID).collect(Collectors.toList());

        if(studentIDs.isEmpty()) {
            nextEvent.setEventOutcome(STUDENTS_FOUND);
            nextEvent.setEventPayload("[]");
            this.handleEvent(nextEvent);
        } else {
            nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(studentIDs));
            this.postMessageToTopic(SagaTopicsEnum.STUDENT_API_TOPIC.toString(), nextEvent);
            log.info("message sent to STUDENT_API_TOPIC for {} Event. :: {}", GET_STUDENTS.toString(), saga.getSagaId());
        }
    }

    protected void saveReports(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SAVE_REPORTS.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchReturnFilesSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        getResponseFileGeneratorService().saveReports(event.getEventPayload(),
          mapper.toModel(penRequestBatchReturnFilesSagaData.getPenRequestBatch()), penRequestBatchReturnFilesSagaData.getPenRequestBatchStudents(), penRequestBatchReturnFilesSagaData.getStudents());

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(SAVE_REPORTS)
          .eventOutcome(REPORTS_SAVED)
          .build();
        this.handleEvent(nextEvent);
    }

    protected void sendArchivedEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData, EventType eventType) throws JsonProcessingException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(eventType.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchReturnFilesSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
          .fromEmail(this.getPenCoordinatorProperties().getFromEmail())
          .mincode(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getMincode())
          .submissionNumber(penRequestBatchReturnFilesSagaData.getPenRequestBatch().getSubmissionNumber())
          .schoolName(penRequestBatchReturnFilesSagaData.getSchoolName())
          .build();
        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .replyTo(this.getTopicToSubscribe())
          .build();

        //set toEmail and email type depending on whether a penCoordinator exists for the mincode
        if(eventType.equals(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT)) {
            nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
            penRequestBatchArchivedEmailEvent.setToEmail(this.getPenCoordinatorProperties().getFromEmail());
        } else {
            nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
            penRequestBatchArchivedEmailEvent.setToEmail(penRequestBatchReturnFilesSagaData.getPenCordinatorEmail());
        }
        nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent));

        this.postMessageToTopic(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for {} Event. :: {}", eventType, saga.getSagaId());
    }

    protected boolean hasPenCoordinatorEmail(BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
        return !this.hasNoPenCoordinatorEmail(penRequestBatchReturnFilesSagaData);
    }

    protected boolean hasNoPenCoordinatorEmail(BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) {
        return penRequestBatchReturnFilesSagaData.getPenCordinatorEmail() == null || penRequestBatchReturnFilesSagaData.getPenCordinatorEmail().isEmpty();
    }

    protected void sendHasNoCoordinatorEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
    }

    protected void sendHasCoordinatorEmail(Event event, Saga saga, BasePenRequestBatchReturnFilesSagaData penRequestBatchReturnFilesSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchReturnFilesSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    }

    protected String getPenCoordinatorEmail(PenRequestBatchEntity penRequestBatchEntity) {
        try {
            var penCoordinatorEmailOptional = getPenCoordinatorService().getPenCoordinatorEmailByMinCode(penRequestBatchEntity.getMincode());
            return penCoordinatorEmailOptional.orElse(null);
        } catch (NullPointerException e) {
            log.error("Error while trying to get get pen coordinator email. The pen coordinator map is null", e);
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
