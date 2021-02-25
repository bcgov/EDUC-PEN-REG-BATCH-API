package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_NEW;

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
  public PenReqBatchNewPenOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestBatchUserActionsSagaData.class,
        PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
        .begin(GET_NEXT_PEN_NUMBER, this::getNextPenNumber)
        .step(GET_NEXT_PEN_NUMBER, NEXT_PEN_NUMBER_RETRIEVED, CREATE_STUDENT, this::createStudent)
        .step(CREATE_STUDENT, STUDENT_ALREADY_EXIST, this::isStudentPossibleMatchAddRequired, ADD_POSSIBLE_MATCH, this::addPossibleMatchesToStudent)
        .step(CREATE_STUDENT, STUDENT_CREATED, this::isStudentPossibleMatchAddRequired, ADD_POSSIBLE_MATCH, this::addPossibleMatchesToStudent)
        .step(CREATE_STUDENT, STUDENT_ALREADY_EXIST, this::isStudentPossibleMatchAddNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(CREATE_STUDENT, STUDENT_CREATED, this::isStudentPossibleMatchAddNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(ADD_POSSIBLE_MATCH, POSSIBLE_MATCH_ADDED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private void addPossibleMatchesToStudent(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    final String studentID;
    // if PEN number is already existing then student-api will return the student-id
    // this scenario might occur during replay when message could not reach batch api from student-api and batch api retried the same flow.
    if (event.getEventOutcome() == STUDENT_ALREADY_EXIST) {
      studentID = event.getEventPayload();
    } else {
      final Student student = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());
      studentID = student.getStudentID();
    }
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_POSSIBLE_MATCH.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final var possibleMatches = penRequestBatchUserActionsSagaData
        .getMatchedStudentIDList().stream()
        .map(matchedStudentID -> PossibleMatch.builder()
            .createUser(penRequestBatchUserActionsSagaData.getCreateUser())
            .updateUser(penRequestBatchUserActionsSagaData.getUpdateUser())
            .studentID(studentID)
            .matchedStudentID(matchedStudentID)
            .matchReasonCode(TwinReasonCodes.PENCREATE.getCode())
            .build()).collect(Collectors.toList());
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(ADD_POSSIBLE_MATCH)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(possibleMatches))
        .build();
    this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for ADD_POSSIBLE_MATCH Event.");
  }

  private boolean isStudentPossibleMatchAddRequired(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return !CollectionUtils.isEmpty(penRequestBatchUserActionsSagaData.getMatchedStudentIDList());
  }

  private boolean isStudentPossibleMatchAddNotRequired(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return CollectionUtils.isEmpty(penRequestBatchUserActionsSagaData.getMatchedStudentIDList());
  }

  /**
   * Get the next PEN number.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch student saga data
   */
  public void getNextPenNumber(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_NEXT_PEN_NUMBER.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final var transactionID = saga.getSagaId().toString();
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(GET_NEXT_PEN_NUMBER)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(transactionID)
        .build();
    this.postMessageToTopic(PEN_SERVICES_API_TOPIC.toString(), nextEvent);
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
  public void createStudent(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    final var pen = event.getEventPayload();
    final var student = studentMapper.toStudent(penRequestBatchUserActionsSagaData);
    student.setPen(pen);
    student.setDemogCode("A");
    student.setHistoryActivityCode(REQ_NEW.getCode());
    penRequestBatchUserActionsSagaData.setAssignedPEN(pen);
    saga.setSagaState(CREATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchUserActionsSagaData));
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(CREATE_STUDENT)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(student))
        .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
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
  protected PenRequestBatchStudent updateSagaDataAndCreatePRBStudent(final Event event, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    final var student = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());

    final var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUserActionsSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(USR_NEW_PEN.getCode());
    prbStudent.setStudentID(student.getStudentID());
    prbStudent.setAssignedPEN(penRequestBatchUserActionsSagaData.getAssignedPEN());
    prbStudent.setRecordNumber(penRequestBatchUserActionsSagaData.getRecordNumber());

    penRequestBatchUserActionsSagaData.setStudentID(student.getStudentID());
    return prbStudent;
  }


}
