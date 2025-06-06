package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.properties.DataManagementUnitProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.*;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchRepostReportsFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportGenerationEventPayload;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_REPOST_REPORTS_TOPIC;

@Slf4j
@Component
public class PenRequestBatchRepostReportsOrchestrator extends BaseReturnFilesOrchestrator<PenRequestBatchRepostReportsFilesSagaData> {

    /**
     * Instantiates a new orchestrator.
     *
     * @param sagaService      the saga service
     * @param messagePublisher the message publisher
     * @param penRequestBatchService the pen request batch service
     * @param studentRegistrationContactService  the student registration contact service
     * @param dataManagementUnitProperties the data management unit properties
     */
    public PenRequestBatchRepostReportsOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                                    PenRequestBatchService penRequestBatchService,
                                                    StudentRegistrationContactService studentRegistrationContactService,
                                                    DataManagementUnitProperties dataManagementUnitProperties,
                                                    ResponseFileGeneratorService responseFileGeneratorService,
                                                    PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService,
                                                    RestUtils restUtils) {
        super(sagaService, messagePublisher, PenRequestBatchRepostReportsFilesSagaData.class,
          PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA.toString(), PEN_REQUEST_BATCH_REPOST_REPORTS_TOPIC.toString(),
          penRequestBatchService, studentRegistrationContactService, dataManagementUnitProperties, responseFileGeneratorService,
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
          .step(GET_STUDENTS, STUDENTS_FOUND, this::isNotSupportingPDFGeneration, SAVE_REPORTS, this::saveReportsWithoutPDF)
          .step(GET_STUDENTS, STUDENTS_FOUND, this::isSupportingPDFGeneration, GENERATE_PEN_REQUEST_BATCH_REPORTS, this::generatePDFReport)
          .step(GENERATE_PEN_REQUEST_BATCH_REPORTS, ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED, SAVE_REPORTS, this::saveReports)
          .step(SAVE_REPORTS, REPORTS_SAVED, this::hasStudentRegistrationContactEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, this::sendHasStudentRegistrationContactEmail)
          .step(SAVE_REPORTS, REPORTS_SAVED, this::hasNoStudentRegistrationContactEmail, NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, this::sendHasNoStudentRegistrationContactEmail)
          .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT, ARCHIVE_EMAIL_SENT)
          .or()
          .end(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT, ARCHIVE_EMAIL_SENT)
          .or()
          .end(GET_STUDENTS, STUDENTS_NOT_FOUND, this::logStudentsNotFound)
          .or()
          .end(GATHER_REPORT_DATA, PEN_REQUEST_BATCH_NOT_FOUND, this::logPenRequestBatchNotFound);
    }

  private void generatePDFReport(Event event, Saga saga, PenRequestBatchRepostReportsFilesSagaData penRequestBatchRepostReportsFilesSagaData) throws IOException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    List<Student> students = obMapper.readValue(event.getEventPayload(), new TypeReference<>(){});
    penRequestBatchRepostReportsFilesSagaData.setStudents(event,students);
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchRepostReportsFilesSagaData)); // save the updated payload to DB...
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GENERATE_PEN_REQUEST_BATCH_REPORTS)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(
        ReportGenerationEventPayload.builder()
          .reportType("PEN_REG_BATCH_RESPONSE_REPORT")
          .reportExtension("pdf")
          .reportName(penRequestBatchRepostReportsFilesSagaData.getPenRequestBatch().getSubmissionNumber())
          .data(reportMapper.toReportData(penRequestBatchRepostReportsFilesSagaData))
          .build()))
      .build();
    this.postMessageToTopic(SagaTopicsEnum.PEN_REPORT_GENERATION_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REPORT_GENERATION_API_TOPIC for {} Event. :: {}", GENERATE_PEN_REQUEST_BATCH_REPORTS, saga.getSagaId());
  }

  private void saveReportsWithoutPDF(Event event, Saga saga, PenRequestBatchRepostReportsFilesSagaData penRequestBatchRepostReportsFilesSagaData) throws IOException, TimeoutException, InterruptedException {
    SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(SAVE_REPORTS.toString());
    List<Student> students = obMapper.readValue(event.getEventPayload(), new TypeReference<>(){});
    penRequestBatchRepostReportsFilesSagaData.setStudents(event,students);
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchRepostReportsFilesSagaData)); // save the updated payload to DB...
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    this.getResponseFileGeneratorService().saveReports(mapper.toModel(penRequestBatchRepostReportsFilesSagaData.getPenRequestBatch()),
      penRequestBatchRepostReportsFilesSagaData.getPenRequestBatchStudents(),
      penRequestBatchRepostReportsFilesSagaData.getStudents(),
      reportMapper.toReportData(penRequestBatchRepostReportsFilesSagaData));

    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(SAVE_REPORTS)
      .eventOutcome(REPORTS_SAVED)
      .build();
    this.handleEvent(nextEvent);
  }

}
