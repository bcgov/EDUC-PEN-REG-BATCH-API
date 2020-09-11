package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.MatchAlgorithmStatusCode;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen req batch student orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchStudentOrchestrator extends BaseOrchestrator<PenRequestBatchStudentSagaData> {
  /**
   * The constant studentMapper.
   */
  private static final StudentMapper studentMapper = StudentMapper.mapper;
  /**
   * The Pen request batch service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchService penRequestBatchService;

  /**
   * The Pen request batch service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentService penRequestBatchStudentService;

  /**
   * Instantiates a new Pen req batch student orchestrator.
   *
   * @param sagaService                   the saga service
   * @param messagePublisher              the message publisher
   * @param messageSubscriber             the message subscriber
   * @param taskScheduler                 the task scheduler
   * @param penRequestBatchService        the pen request batch service
   * @param penRequestBatchStudentService the pen request batch student service
   */
  @Autowired
  public PenReqBatchStudentOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler, PenRequestBatchService penRequestBatchService, PenRequestBatchStudentService penRequestBatchStudentService) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, PenRequestBatchStudentSagaData.class, PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString());
    this.penRequestBatchService = penRequestBatchService;
    this.penRequestBatchStudentService = penRequestBatchStudentService;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
      .step(READ_FROM_TOPIC, READ_FROM_TOPIC_SUCCESS, PROCESS_PEN_MATCH, this::processPenMatch)
      .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
      .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::executeUpdateStudent)
      .step(GET_STUDENT, STUDENT_NOT_FOUND, CREATE_STUDENT, this::executeCreateStudent)
      .step(CREATE_STUDENT, STUDENT_CREATED, MARK_SAGA_COMPLETE, this::markSagaComplete)
      .step(UPDATE_STUDENT, STUDENT_UPDATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * Execute create student.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   */
  private void executeCreateStudent(Event event, Saga saga, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Student student = studentMapper.toStudent(penRequestBatchStudentSagaData, penRequestBatchStudentSagaData.getPenMatchResult());// get the student data from saga payload.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event. :: {}", saga.getSagaId());
    delegateMessagePostingForStudent(saga, student, CREATE_STUDENT);
  }

  /**
   * Execute update student.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  private void executeUpdateStudent(Event event, Saga saga, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws JsonProcessingException {
    Student student = studentMapper.toStudent(penRequestBatchStudentSagaData, penRequestBatchStudentSagaData.getPenMatchResult()); // get the student data from saga payload.
    Student studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());
    student.setStudentID(studentDataFromEventResponse.getStudentID()); // update the student ID so that update call will have proper identifier.
    penRequestBatchStudentSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
    saga.setSagaState(UPDATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData));
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event. :: {}", saga.getSagaId());
    delegateMessagePostingForStudent(saga, student, UPDATE_STUDENT);
  }


  /**
   * Process pen match results.
   * this will go through and process the result based on the logic provided here.
   * <pre>
   *    IF the outcome is that the request is matched to an existing Student record:
   *      Update the PEN Request Student Status Code on the PEN Request Student record to: SYSMATCHED
   *      Update the Student ID foreign key on the PEN Request Student record to specify the matched Student record.
   *      Update the Student table for the matched Student record (Student ID above), updating the values of the following fields, based on the values in the PEN Request Student and the PEN Request Batch record:
   *      Mincode (from the PEN Request Batch record)
   *      Local ID
   *      Student Grade Code
   *      Postal Code
   *      TBD: Do any of the other demographic values get updated? For K-12? For PSIs?
   *    ELSEIF the outcome is that a new Student/PEN record is created to fulfill the request:
   *      Insert a new record in the Student table based on the data from the PEN Request Student record (and Mincode from the PEN Request Batch record). This will have a new Student ID.
   *      Update the Student ID foreign key on the PEN Request Student record to specify the new Student record.
   *      Update the PEN Request Student Status Code on the PEN Request Student record to: SYSNEWPEN
   *    ELSEIF the outcome is uncertain, such that (Submitted PEN Status Code = F1 OR PEN Match Phase 1 Status Code = F1):
   *      Update the value of Questionable Match Student ID on the PEN Request Student record.
   *      Out of Scope: Run phase 2 (New Match) of the PEN match algorithm. The status on the request will remain as LOADED.
   *    ELSE the request requires manual review:
   *      Update the PEN Request Student Status Code on the PEN Request Student record to: NEWFIXABLE
   * </pre>
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  private void processPenMatchResults(Event event, Saga saga, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH_RESULTS.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, event.getEventPayload());
    penRequestBatchStudentSagaData.setPenMatchResult(penMatchResult); // update the original payload with response from PEN_MATCH_API
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var algorithmStatusCode = MatchAlgorithmStatusCode.valueOf(penMatchResult.getPenStatus());
    switch (algorithmStatusCode) {
      case D1:

        break;
      default:
        var penRequestBatchStudentEntity = getPenRequestBatchStudentService().getStudentById(penRequestBatchStudentSagaData.getPenRequestBatchID(), penRequestBatchStudentSagaData.getPenRequestBatchStudentID());
        penRequestBatchStudentEntity.setPenRequestBatchStudentStatusCode(PenRequestBatchStudentStatusCodes.NEW_FIXABLE.getCode());
        getPenRequestBatchStudentService().saveAttachedEntity(penRequestBatchStudentEntity);
        break;
    }

  }

  /**
   * Process pen match.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   */
  private void processPenMatch(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var eventPayload = JsonUtil.getJsonString(penRequestBatchStudentSagaData);
    if (eventPayload.isPresent()) {
      Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(PROCESS_PEN_MATCH)
        .replyTo(getTopicToSubscribe())
        .eventPayload(eventPayload.get())
        .build();
      postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
      log.info("message sent to PEN_MATCH_API_TOPIC for PROCESS_PEN_MATCH Event. :: {}", saga.getSagaId());
    } else {
      log.error("event payload is not present this should not have happened. :: {}", saga.getSagaId());
    }
  }
}
