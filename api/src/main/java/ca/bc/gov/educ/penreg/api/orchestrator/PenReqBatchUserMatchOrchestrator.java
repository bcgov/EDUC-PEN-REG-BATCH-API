package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.StudentTwin;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_MATCHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
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
        .step(UPDATE_STUDENT, STUDENT_UPDATED, CHECK_STUDENT_TWIN_ADD, this::checkStudentTwinAdditionRequired)
        .step(CHECK_STUDENT_TWIN_ADD, STUDENT_TWIN_ADD_REQUIRED, ADD_STUDENT_TWINS, this::addTwinRecordsToStudent)
        .step(CHECK_STUDENT_TWIN_ADD, STUDENT_TWIN_ADD_NOT_REQUIRED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(ADD_STUDENT_TWINS, STUDENT_TWINS_ADDED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private void checkStudentTwinAdditionRequired(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws InterruptedException, TimeoutException, IOException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CHECK_STUDENT_TWIN_ADD.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    handleEvent(Event.builder()
                     .sagaId(saga.getSagaId())
                     .eventType(CHECK_STUDENT_TWIN_ADD)
                     .eventOutcome(CollectionUtils.isEmpty(penRequestBatchUserActionsSagaData.getTwinStudentIDs()) ? STUDENT_TWIN_ADD_NOT_REQUIRED : STUDENT_TWIN_ADD_REQUIRED)
                     .build());
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
  protected void addTwinRecordsToStudent(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_STUDENT_TWINS.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var studentTwins = penRequestBatchUserActionsSagaData
        .getTwinStudentIDs().stream()
        .map(twinStudentID -> StudentTwin.builder()
                                         .createUser(penRequestBatchUserActionsSagaData.getCreateUser())
                                         .updateUser(penRequestBatchUserActionsSagaData.getUpdateUser())
                                         .studentID(penRequestBatchUserActionsSagaData.getStudentID())
                                         .twinStudentID(twinStudentID)
                                         .studentTwinReasonCode(TwinReasonCodes.PEN_MATCH.getCode())
                                         .build()).collect(Collectors.toList());
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(ADD_STUDENT_TWINS)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(JsonUtil.getJsonStringFromObject(studentTwins))
                           .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for ADD_STUDENT_TWINS Event.");
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
   * Mincode
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
