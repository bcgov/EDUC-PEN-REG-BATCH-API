package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
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
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.service.ResponseFileGeneratorService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC;
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
    private final ResponseFileGeneratorService responseFileGeneratorService;

    @Getter(PRIVATE)
    private final PenCoordinatorService penCoordinatorService;

    @Getter(PRIVATE)
    private final PenCoordinatorProperties penCoordinatorProperties;

    private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
    private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;

    private static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;

    private final ObjectMapper obMapper = new ObjectMapper();

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
                                                       PenCoordinatorProperties penCoordinatorProperties,
                                                       ResponseFileGeneratorService responseFileGeneratorService) {
        super(sagaService, messagePublisher, PenRequestBatchArchiveAndReturnSagaData.class,
                PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC.toString());
        this.penRequestBatchService = penRequestBatchService;
        this.penCoordinatorService = penCoordinatorService;
        this.penCoordinatorProperties = penCoordinatorProperties;
        this.responseFileGeneratorService = responseFileGeneratorService;
    }

    /**
     * Populate steps to execute map.
     */
    @Override
    public void populateStepsToExecuteMap() {
        stepBuilder()
                .begin(GATHER_REPORT_DATA, this::gatherReportData)
                .step(GATHER_REPORT_DATA, REPORT_DATA_GATHERED, GET_STUDENTS, this::getStudents)
                .step(GET_STUDENTS, STUDENTS_FOUND, UPDATE_PEN_REQUEST_BATCH, this::archivePenRequestBatch)
                .step(UPDATE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_UPDATED, GENERATE_PEN_REQUEST_BATCH_REPORTS, this::generatePDFReport)
                .step(GENERATE_PEN_REQUEST_BATCH_REPORTS, ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED, SAVE_REPORTS, this::saveReports)
                .step(SAVE_REPORTS, REPORTS_SAVED, this::hasPenCoordinatorEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, this::sendHasCoordinatorEmail)
                .step(SAVE_REPORTS, REPORTS_SAVED, this::hasNoPenCoordinatorEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, this::sendHasNoCoordinatorEmail)
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(GET_STUDENTS, STUDENTS_NOT_FOUND, this::logStudentsNotFound)
                .or()
                .end(UPDATE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound)
                .or()
                .end(GATHER_REPORT_DATA, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound);
    }

    private void gatherReportData(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GATHER_REPORT_DATA.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var penRequestBatch = getPenRequestBatchService().findById(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());
        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(GATHER_REPORT_DATA).replyTo(this.getTopicToSubscribe()).build();

        if(penRequestBatch.isPresent()) {
            List<PenRequestBatchStudent> studentRequests = penRequestBatch.get().getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList());
            penRequestBatchArchiveAndReturnSagaData.setPenRequestBatchStudents(studentRequests);
            penRequestBatchArchiveAndReturnSagaData.setPenCordinatorEmail(this.getPenCoordinatorEmail(penRequestBatch.get()));
            penRequestBatchArchiveAndReturnSagaData.setFromEmail(penCoordinatorProperties.getFromEmail());
            penRequestBatchArchiveAndReturnSagaData.setTelephone(penCoordinatorProperties.getTelephone());
            penRequestBatchArchiveAndReturnSagaData.setFacsimile(penCoordinatorProperties.getFacsimile());
            penRequestBatchArchiveAndReturnSagaData.setMailingAddress(penCoordinatorProperties.getMailingAddress());
            saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
            nextEvent.setEventOutcome(REPORT_DATA_GATHERED);
        } else {
            log.error("PenRequestBatch not found during archive and return saga. This is not expected :: {}", penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());
            nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
        }
        this.handleEvent(nextEvent);
    }

    private void getStudents(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, TimeoutException, InterruptedException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GET_STUDENTS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.GET_STUDENTS).replyTo(this.getTopicToSubscribe()).build();

        List<String> studentIDs = penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchStudents().stream()
                .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode()))
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

    private void archivePenRequestBatch(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_PEN_REQUEST_BATCH.toString());
        List<Student> matchedStudents = obMapper.readValue(event.getEventPayload(), new TypeReference<>(){});
        penRequestBatchArchiveAndReturnSagaData.setMatchedStudents(matchedStudents);
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        Event nextEvent = Event.builder().sagaId(saga.getSagaId()).eventType(EventType.UPDATE_PEN_REQUEST_BATCH).build();
        var penRequestBatchEntityOptional = getPenRequestBatchService().findById(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());

        if(penRequestBatchEntityOptional.isPresent()) {
            PenRequestBatchEntity penRequestBatch = penRequestBatchEntityOptional.get();
            if (StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())
                    || StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode())) {
                penRequestBatch.setPenRequestBatchStatusCode(REARCHIVED.getCode());
            } else {
                penRequestBatch.setPenRequestBatchStatusCode(ARCHIVED.getCode());
            }
            if (penRequestBatchArchiveAndReturnSagaData.getUpdateUser() == null) {
                penRequestBatch.setUpdateUser(PEN_REQUEST_BATCH_API);
            } else {
                penRequestBatch.setUpdateUser(penRequestBatchArchiveAndReturnSagaData.getUpdateUser());
            }
            penRequestBatch.setProcessDate(LocalDateTime.now());
            try {
                getPenRequestBatchService().updatePenRequestBatch(penRequestBatch, penRequestBatch.getPenRequestBatchID());
                penRequestBatchArchiveAndReturnSagaData.setPenRequestBatch(mapper.toStructure(penRequestBatch));
                saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
                nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(penRequestBatch)));// need to convert to structure MANDATORY otherwise jackson will break.
                nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_UPDATED);
            } catch (EntityNotFoundException ex) {
                log.error("PenRequestBatch not found while trying to update it. This should not happen :: ", ex);
                nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
            }
        } else {
            log.error("PenRequestBatch not found while trying to update it. This should not happen :: " + penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID());
            nextEvent.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
        }
        this.handleEvent(nextEvent);
    }

    private void generatePDFReport(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(GENERATE_PEN_REQUEST_BATCH_REPORTS)
                .replyTo(this.getTopicToSubscribe())
                .eventPayload(JsonUtil.getJsonStringFromObject(reportMapper.toReportData(penRequestBatchArchiveAndReturnSagaData)))
                .build();
        this.postMessageToTopic(SagaTopicsEnum.PEN_REPORT_GENERATION_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PEN_REPORT_GENERATION_API_TOPIC for {} Event. :: {}", GENERATE_PEN_REQUEST_BATCH_REPORTS.toString(), saga.getSagaId());
    }

    private void saveReports(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SAVE_REPORTS.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        getPenRequestBatchService().saveReports(event.getEventPayload(),
                mapper.toModel(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch()));

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(SAVE_REPORTS)
                .eventOutcome(REPORTS_SAVED)
                .build();
        this.handleEvent(nextEvent);
    }

    private void sendArchivedEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData, EventType eventType) throws JsonProcessingException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(eventType.toString());
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
                .fromEmail(this.getPenCoordinatorProperties().getFromEmail())
                .mincode(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch().getMincode())
                .submissionNumber(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch().getSubmissionNumber())
                .schoolName(penRequestBatchArchiveAndReturnSagaData.getSchoolName())
                .build();
        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .replyTo(this.getTopicToSubscribe())
                .build();

        //set toEmail and email type depending on whether a penCoordinator exists for the mincode
        if(penRequestBatchArchiveAndReturnSagaData.getPenCordinatorEmail() != null) {
            nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
            penRequestBatchArchivedEmailEvent.setToEmail(penRequestBatchArchiveAndReturnSagaData.getPenCordinatorEmail());
        } else {
            nextEvent.setEventType(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
            penRequestBatchArchivedEmailEvent.setToEmail(this.getPenCoordinatorProperties().getFromEmail());
        }
        nextEvent.setEventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent));

        this.postMessageToTopic(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for {} Event. :: {}", eventType, saga.getSagaId());
    }

    private boolean hasPenCoordinatorEmail(PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) {
        return penRequestBatchArchiveAndReturnSagaData.getPenCordinatorEmail() != null;
    }

    private boolean hasNoPenCoordinatorEmail(PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) {
        return !this.hasPenCoordinatorEmail(penRequestBatchArchiveAndReturnSagaData);
    }

    private void sendHasNoCoordinatorEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchArchiveAndReturnSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
    }

    private void sendHasCoordinatorEmail(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        this.sendArchivedEmail(event, saga, penRequestBatchArchiveAndReturnSagaData, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    }

    private String getPenCoordinatorEmail(PenRequestBatchEntity penRequestBatchEntity) {
        try {
            var penCoordinatorEmailOptional = getPenCoordinatorService().getPenCoordinatorEmailByMinCode(penRequestBatchEntity.getMincode());
            return penCoordinatorEmailOptional.orElse(null);
        } catch (NullPointerException e) {
            log.error("Error while trying to get get pen coordinator email. The pen coordinator map is null", e);
            return null;
        }
    }

    private void logStudentsNotFound(final Event event, final Saga saga, final PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) {
        log.error("Student record(s) were not found. This should not happen. Please check the student api. :: {}", saga.getSagaId());
    }

    private void logPenRequestBatchNotFound(final Event event, final Saga saga, final PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) {
        log.error("Pen request batch record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
    }
}
