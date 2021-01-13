package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_MATCHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_MATCH;

/**
 * The type Pen req batch user match orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchUserMatchOrchestrator extends BaseUserActionsOrchestrator<PenRequestBatchUserActionsSagaData> {


  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   */
  public PenReqBatchUserMatchOrchestrator(SagaService sagaService, MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestBatchUserActionsSagaData.class,
        PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_TOPIC.toString());
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .begin(GET_STUDENT, this::getStudentByPen)
        .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::updateStudent)
        .step(UPDATE_STUDENT, STUDENT_UPDATED, this::isPossibleMatchAddRequired, ADD_POSSIBLE_MATCH, this::addPossibleMatchToStudent)
        .step(UPDATE_STUDENT, STUDENT_UPDATED, this::isPossibleMatchAddNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(ADD_POSSIBLE_MATCH, POSSIBLE_MATCH_ADDED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private boolean isPossibleMatchAddRequired(PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return !isPossibleMatchAddNotRequired(penRequestBatchUserActionsSagaData);
  }

  private boolean isPossibleMatchAddNotRequired(PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return CollectionUtils.isEmpty(penRequestBatchUserActionsSagaData.getMatchedStudentIDList());
  }

  /**
   * this method expects that the twin ids provided in the payload here is already validated.
   * Add twin records to student.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void addPossibleMatchToStudent(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_POSSIBLE_MATCH.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var possibleMatches = penRequestBatchUserActionsSagaData
        .getMatchedStudentIDList().stream()
        .map(matchedStudentID -> PossibleMatch.builder()
            .createUser(penRequestBatchUserActionsSagaData.getCreateUser())
            .updateUser(penRequestBatchUserActionsSagaData.getUpdateUser())
            .studentID(penRequestBatchUserActionsSagaData.getStudentID())
            .matchedStudentID(matchedStudentID)
            .matchReasonCode(TwinReasonCodes.PEN_MATCH.getCode())
            .build()).collect(Collectors.toList());
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(ADD_POSSIBLE_MATCH)
        .replyTo(getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(possibleMatches))
        .build();
    postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for ADD_POSSIBLE_MATCH Event.");
  }

  /**
   * Gets student by pen.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   */
  protected void getStudentByPen(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(GET_STUDENT.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(GET_STUDENT)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(penRequestBatchUserActionsSagaData.getAssignedPEN())
                           .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT Event.");
  }

  /**
   * the following attributes on the matched student record get updated based on the incoming PEN Request
   * mincode
   * Local ID
   * Student Grade Code
   * Postal Code
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void updateStudent(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_STUDENT.toString()); // set current event as saga state.
    Student studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());
    studentDataFromEventResponse.setUpdateUser(penRequestBatchUserActionsSagaData.getUpdateUser());
    studentDataFromEventResponse.setMincode(penRequestBatchUserActionsSagaData.getMincode());
    studentDataFromEventResponse.setLocalID(penRequestBatchUserActionsSagaData.getLocalID());
    studentDataFromEventResponse.setGradeCode(penRequestBatchUserActionsSagaData.getGradeCode());
    studentDataFromEventResponse.setPostalCode(penRequestBatchUserActionsSagaData.getPostalCode());
    studentDataFromEventResponse.setHistoryActivityCode(REQ_MATCH.getCode());
    penRequestBatchUserActionsSagaData.setStudentID(studentDataFromEventResponse.getStudentID());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(UPDATE_STUDENT)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(JsonUtil.getJsonStringFromObject(studentDataFromEventResponse))
                           .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
  }

  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event                              the event
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   * @return the pen request batch student
   */
  @Override
  protected PenRequestBatchStudent updateSagaDataAndCreatePRBStudent(Event event, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUserActionsSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(USR_MATCHED.getCode());
    prbStudent.setAssignedPEN(penRequestBatchUserActionsSagaData.getAssignedPEN());
    prbStudent.setStudentID(penRequestBatchUserActionsSagaData.getStudentID());
    prbStudent.setUpdateUser(penRequestBatchUserActionsSagaData.getUpdateUser());
    return prbStudent;
  }
}
