package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.properties.PenCoordinatorProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.*;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchive;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportGenerationEventPayload;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
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
                                                       PenCoordinatorProperties penCoordinatorProperties,
                                                       ResponseFileGeneratorService responseFileGeneratorService,
                                                       PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService,
                                                       RestUtils restUtils) {
        super(sagaService, messagePublisher, PenRequestBatchArchiveAndReturnSagaData.class,
          PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_TOPIC.toString(),
          penRequestBatchService, penCoordinatorService, penCoordinatorProperties, responseFileGeneratorService,
          penRequestBatchStudentValidationIssueService, restUtils);
    }

    /**
     * Populate steps to execute map.
     */
    @Override
    public void populateStepsToExecuteMap() {
        stepBuilder()
                .begin(GATHER_REPORT_DATA, this::gatherReportData)
                .step(GATHER_REPORT_DATA, REPORT_DATA_GATHERED, GET_STUDENTS, this::getStudents)
                .step(GET_STUDENTS, STUDENTS_FOUND, ARCHIVE_PEN_REQUEST_BATCH, this::archivePenRequestBatch)
                .step(ARCHIVE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_UPDATED, this::isNotSupportingPDFGeneration, SAVE_REPORTS, this::saveReportsWithoutPDF)
                .step(ARCHIVE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_UPDATED, this::isSupportingPDFGeneration, GENERATE_PEN_REQUEST_BATCH_REPORTS, this::generatePDFReport)
                .step(GENERATE_PEN_REQUEST_BATCH_REPORTS, ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED, SAVE_REPORTS, this::saveReports)
                .step(SAVE_REPORTS, REPORTS_SAVED, this::hasPenCoordinatorEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, this::sendHasCoordinatorEmail)
                .step(SAVE_REPORTS, REPORTS_SAVED, this::hasNoPenCoordinatorEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, this::sendHasNoCoordinatorEmail)
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, ARCHIVE_EMAIL_SENT)
                .or()
                .end(GET_STUDENTS, STUDENTS_NOT_FOUND, this::logStudentsNotFound)
                .or()
                .end(ARCHIVE_PEN_REQUEST_BATCH, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound)
                .or()
                .end(GATHER_REPORT_DATA, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound);
    }

    private void archivePenRequestBatch(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(ARCHIVE_PEN_REQUEST_BATCH.toString());
        List<Student> students = obMapper.readValue(event.getEventPayload(), new TypeReference<>(){});
        penRequestBatchArchiveAndReturnSagaData.setStudents(event,students);
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var penRequestBatchArchive = PenRequestBatchArchive.builder()
          .penRequestBatchID(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID())
          .updateUser(penRequestBatchArchiveAndReturnSagaData.getUpdateUser())
          .build();

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(EventType.ARCHIVE_PEN_REQUEST_BATCH)
          .replyTo(this.getTopicToSubscribe())
          .eventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchive))
          .build();
        this.postMessageToTopic(PEN_REQUEST_BATCH_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PEN_REQUEST_BATCH_API_TOPIC for ARCHIVE_PEN_REQUEST_BATCH Event. :: {}", saga.getSagaId());
    }

    private void generatePDFReport(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws JsonProcessingException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
        penRequestBatchArchiveAndReturnSagaData.setPenRequestBatch(JsonUtil.getJsonObjectFromString(PenRequestBatch.class, event.getEventPayload()));
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(GENERATE_PEN_REQUEST_BATCH_REPORTS)
                .replyTo(this.getTopicToSubscribe())
                .eventPayload(JsonUtil.getJsonStringFromObject(
                        ReportGenerationEventPayload.builder()
                                .reportType("PEN_REG_BATCH_RESPONSE_REPORT")
                                .reportExtension("pdf")
                                .reportName(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch().getSubmissionNumber())
                                .data(reportMapper.toReportData(penRequestBatchArchiveAndReturnSagaData))
                                .build()))
                .build();
        this.postMessageToTopic(SagaTopicsEnum.PEN_REPORT_GENERATION_API_TOPIC.toString(), nextEvent);
        log.info("message sent to PEN_REPORT_GENERATION_API_TOPIC for {} Event. :: {}", GENERATE_PEN_REQUEST_BATCH_REPORTS.toString(), saga.getSagaId());
    }

    private void saveReportsWithoutPDF(Event event, Saga saga, PenRequestBatchArchiveAndReturnSagaData penRequestBatchArchiveAndReturnSagaData) throws IOException, InterruptedException, TimeoutException {
        SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SAVE_REPORTS.toString());
        penRequestBatchArchiveAndReturnSagaData.setPenRequestBatch(JsonUtil.getJsonObjectFromString(PenRequestBatch.class, event.getEventPayload()));
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchiveAndReturnSagaData)); // save the updated payload to DB...
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
        if(penRequestBatchArchiveAndReturnSagaData.getStudents() == null){
          log.info("students in saga data is null or empty for batch id :: {} and saga id :: {}, setting it from event states table", penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchID(), saga.getSagaId());
          SagaEvent sagaEvent = SagaEvent.builder().sagaEventState(GET_STUDENTS.toString()).sagaEventOutcome(STUDENTS_FOUND.toString()).sagaStepNumber(3).build();
          val sagaEventOptional = this.getSagaService().findSagaEvent(saga,sagaEvent);
          if(sagaEventOptional.isPresent()){
            List<Student> students = obMapper.readValue(sagaEventOptional.get().getSagaEventResponse(), new TypeReference<>(){});
            penRequestBatchArchiveAndReturnSagaData.setStudents(event,students);
          }else{
            throw new PenRegAPIRuntimeException("students not found in event states table for saga id :: "+saga.getSagaId());
          }
        }
        this.getResponseFileGeneratorService().saveReports(mapper.toModel(penRequestBatchArchiveAndReturnSagaData.getPenRequestBatch()),
          penRequestBatchArchiveAndReturnSagaData.getPenRequestBatchStudents(),
          penRequestBatchArchiveAndReturnSagaData.getStudents(),
          reportMapper.toReportData(penRequestBatchArchiveAndReturnSagaData));

        val nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(SAVE_REPORTS)
          .eventOutcome(REPORTS_SAVED)
          .build();
        this.handleEvent(nextEvent);
    }

}
