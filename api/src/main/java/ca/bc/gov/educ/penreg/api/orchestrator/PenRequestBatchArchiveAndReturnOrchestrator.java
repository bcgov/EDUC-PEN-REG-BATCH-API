package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.properties.PenCoordinatorProperties;
import ca.bc.gov.educ.penreg.api.service.PenCoordinatorService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportGenerationEvent;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.ARCHIVED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REARCHIVED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC;

@Slf4j
@Component
public class PenRequestBatchArchiveAndReturnOrchestrator extends BaseReturnFilesOrchestrator<PenRequestBatchArchiveAndReturnSagaData> {

    /**
     * Instantiates a new Base orchestrator.
     *
     * @param sagaService      the saga service
     * @param messagePublisher the message publisher
     * @param penRequestBatchService the pen request batch service
     * @param penCoordinatorService  the pen coordinator service
     * @param penCoordinatorProperties the pen coordinator properties
     */
    public PenRequestBatchArchiveAndReturnOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                                       PenRequestBatchService penRequestBatchService,
                                                       PenCoordinatorService penCoordinatorService,
                                                       PenCoordinatorProperties penCoordinatorProperties) {
        super(sagaService, messagePublisher, PenRequestBatchArchiveAndReturnSagaData.class,
          PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC.toString(),
          penRequestBatchService, penCoordinatorService, penCoordinatorProperties);
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
                .eventPayload(JsonUtil.getJsonStringFromObject(
                        ReportGenerationEvent.builder()
                                .reportType("PEN_REG_BATCH_RESPONSE_REPORT")
                                .reportExtension("pdf")
                                .reportName(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch().getSubmissionNumber())
                                .data(reportMapper.toReportData(penRequestBatchArchiveAndReturnSagaData))
                                .build()))
                .build();
        this.postMessageToTopic(SagaTopicsEnum.PEN_REPORT_GENERATION_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PEN_REPORT_GENERATION_API_TOPIC for {} Event. :: {}", GENERATE_PEN_REQUEST_BATCH_REPORTS.toString(), saga.getSagaId());
    }
}
