package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.USER_NEW;
import static java.util.stream.Collectors.toList;

/**
 * The type Pen req batch student orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchNewPenOrchestrator extends BaseUserActionsOrchestrator<PenRequestBatchUserActionsSagaData> {

  /**
   * Instantiates a new Pen req batch student orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   */
  @Autowired
  public PenReqBatchNewPenOrchestrator(SagaService sagaService, MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestBatchUserActionsSagaData.class,
        PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .begin(GET_NEXT_PEN_NUMBER, this::getNextPenNumber)
        .step(GET_NEXT_PEN_NUMBER, NEXT_PEN_NUMBER_RETRIEVED, CREATE_STUDENT, this::createStudent)
        .step(CREATE_STUDENT, STUDENT_ALREADY_EXIST, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(CREATE_STUDENT, STUDENT_CREATED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  /**
   * Get the next PEN number.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch student saga data
   */
  public void getNextPenNumber(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_NEXT_PEN_NUMBER.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    var transactionID = saga.getSagaId().toString();
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(GET_NEXT_PEN_NUMBER)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(transactionID)
                           .build();
    postMessageToTopic(PEN_SERVICES_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_SERVICES_API_TOPIC for GET_NEXT_PEN_NUMBER Event. :: {}", saga.getSagaId());
  }

  /**
   * Create student record.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  public void createStudent(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    var pen = event.getEventPayload();

    var student = studentMapper.toStudent(penRequestBatchUserActionsSagaData);
    student.setPen(pen);
    student.setDemogCode("A");
    student.setHistoryActivityCode(USER_NEW.getCode());
    student.setStudentTwinAssociations(penRequestBatchUserActionsSagaData.getTwinStudentIDs().stream().map(studentID ->
        new StudentTwinAssociation(studentID, TwinReasonCodes.PENCREATE.getCode())).collect(toList()));

    penRequestBatchUserActionsSagaData.setAssignedPEN(pen);
    saga.setSagaState(CREATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchUserActionsSagaData));
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(CREATE_STUDENT)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(JsonUtil.getJsonStringFromObject(student))
                           .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event. :: {}", saga.getSagaId());
  }


  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event                              the event
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   * @return the pen request batch student
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  protected PenRequestBatchStudent updateSagaDataAndCreatePRBStudent(Event event, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    var student = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());

    var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUserActionsSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(USR_NEW_PEN.getCode());
    prbStudent.setStudentID(student.getStudentID());
    prbStudent.setAssignedPEN(penRequestBatchUserActionsSagaData.getAssignedPEN());

    penRequestBatchUserActionsSagaData.setStudentID(student.getStudentID());
    return prbStudent;
  }



}
