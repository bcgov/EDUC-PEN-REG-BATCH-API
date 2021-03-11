package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.properties.NotificationProperties;
import ca.bc.gov.educ.penreg.api.service.*;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchivedEmailEvent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.ARCHIVED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GENERATE_IDS_REPORT;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GET_PEN_COORDINATOR;
import static ca.bc.gov.educ.penreg.api.constants.EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT;
import static ca.bc.gov.educ.penreg.api.constants.EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_UPDATED;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.IDS_REPORT_GENERATED;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_COORDINATOR_FOUND;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_COORDINATOR_NOT_FOUND;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.ARCHIVE_EMAIL_SENT;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
public class PenRequestBatchArchiveAndReturnOrchestrator extends BaseOrchestrator<PenRequestBatchArchiveAndReturnSagaData> {

    /**
     * The Pen request batch service.
     */
    @Getter(PRIVATE)
    private final PenRequestBatchService penRequestBatchService;

    @Getter(PRIVATE)
    private final PenCoordinatorService penCoordinatorService;

    @Getter(PRIVATE)
    private final NotificationProperties notificationProperties;

    private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;

    /**
     * The constant PEN_REQUEST_BATCH_API.
     */
    String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

    /**
     * Instantiates a new Base orchestrator.
     *
     * @param sagaService      the saga service
     * @param messagePublisher the message publisher
     */
    public PenRequestBatchArchiveAndReturnOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                                       PenRequestBatchService penRequestBatchService,
                                                       PenCoordinatorService penCoordinatorService,
                                                       NotificationProperties notificationProperties) {
        super(sagaService, messagePublisher, PenRequestBatchArchiveAndReturnSagaData.class,
                PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC.toString());
        this.penRequestBatchService = penRequestBatchService;
        this.penCoordinatorService = penCoordinatorService;
        this.notificationProperties = notificationProperties;
    }

    /**
     * Populate steps to execute map.
     */
    @Override
    public void populateStepsToExecuteMap() {
        stepBuilder()
                .begin(UPDATE_PEN_REQUEST_BATCH, this::archivePenRequestBatch)
                .step(UPDATE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_UPDATED, GENERATE_IDS_REPORT, this::generateIDSReport)
                .step(GENERATE_IDS_REPORT, IDS_REPORT_GENERATED, GET_PEN_COORDINATOR, this::getPenCoordinator)
                .step(GET_PEN_COORDINATOR, PEN_COORDINATOR_FOUND, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, this::sendHasCoordinatorEmail)
                .step(GET_PEN_COORDINATOR, PEN_COORDINATOR_NOT_FOUND, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, this::sendHasNoCoordinatorEmail)
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(UPDATE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound);
    }

    private void sendHasNoCoordinatorEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchArchiveAndReturnSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
    }

    private void sendHasCoordinatorEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchArchiveAndReturnSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    }

    private void archivePenRequestBatch(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_PEN_REQUEST_BATCH.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var penRequestBatch = getPenRequestBatchService().findById(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());
        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.UPDATE_PEN_REQUEST_BATCH).build();
        if (penRequestBatch.isPresent()) {
            penRequestBatch.get().setPenRequestBatchStatusCode(ARCHIVED.getCode());
            if (penRequestBatchArchiveAndReturnSagaData.getUpdateUser() == null) {
                penRequestBatch.get().setUpdateUser(PEN_REQUEST_BATCH_API);
            } else {
                penRequestBatch.get().setUpdateUser(penRequestBatchArchiveAndReturnSagaData.getUpdateUser());
            }
            try {
                getPenRequestBatchService().updatePenRequestBatch(penRequestBatch.get(), penRequestBatch.get().getPenRequestBatchID());
                nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(penRequestBatch.get())));// need to convert to structure MANDATORY otherwise jackson will break.
                nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_UPDATED);
            } catch (EntityNotFoundException ex) {
                log.error("PenRequestBatchStudent not found while trying to update it", ex);
                nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
            }
        } else {
            log.error("PenRequestBatch not found while trying to update it. This is not expected :: {}", penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());
            nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
        }
        this.handleEvent(nextEvent);
    }

    private void generateIDSReport(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GENERATE_IDS_REPORT.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        getPenRequestBatchService().createIDSFile(mapper.toModel(JsonUtil.getJsonObjectFromString(PenRequestBatch.class, event.getEventPayload())));

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_IDS_REPORT)
                .eventOutcome(EventOutcome.IDS_REPORT_GENERATED)
                .eventPayload(event.getEventPayload())
                .build();
        this.handleEvent(nextEvent);
    }

    private void getPenCoordinator(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GET_PEN_COORDINATOR.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var penRequestBatch = JsonUtil.getJsonObjectFromString(PenRequestBatch.class, event.getEventPayload());
        var penCoordinatorEmailOptional = getPenCoordinatorService().getPenCoordinatorEmailByMinCode(penRequestBatch.getMincode());
        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.GET_PEN_COORDINATOR).build();
        PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
                .fromEmail(this.getNotificationProperties().getFromEmail())
                .mincode(penRequestBatch.getMincode())
                .submissionNumber(penRequestBatch.getSubmissionNumber())
                .schoolName(penRequestBatch.getSchoolName())
                .build();
        if(penCoordinatorEmailOptional.isPresent()) {
            nextEvent.setEventOutcome(EventOutcome.PEN_COORDINATOR_FOUND);
            penRequestBatchArchivedEmailEvent.setToEmail(penCoordinatorEmailOptional.get());
        } else {
            nextEvent.setEventOutcome(EventOutcome.PEN_COORDINATOR_NOT_FOUND);
            penRequestBatchArchivedEmailEvent.setToEmail(this.getNotificationProperties().getFromEmail());
        }
        nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent));
        this.handleEvent(nextEvent);
    }

    private void sendArchivedEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData, EventType eventType) throws JsonProcessingException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(eventType.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var archiveEmailDataFromEventResponse = JsonUtil.getJsonObjectFromString(PenRequestBatchArchivedEmailEvent.class, event.getEventPayload());
        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(eventType)
                .replyTo(this.getTopicToSubscribe())
                .eventPayload(JsonUtil.getJsonStringFromObject(archiveEmailDataFromEventResponse))
                .build();
        this.postMessageToTopic(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for {} Event. :: {}", eventType, saga.getSagaId());
    }

    private void logPenRequestBatchNotFound(final Event event, final Saga saga, final PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) {
        log.error("Pen request batch record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
    }
}
