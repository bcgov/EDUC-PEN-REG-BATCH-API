package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;

/**
 * The type Pen req batch user unmatch orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchUserUnmatchOrchestrator extends BaseUserActionsOrchestrator<PenRequestBatchUnmatchSagaData> {

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   */
  public PenReqBatchUserUnmatchOrchestrator(SagaService sagaService, MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestBatchUnmatchSagaData.class,
        PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_TOPIC.toString());
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .begin(this::isNotStudentTwinDeleteRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .or()
        .begin(this::isStudentTwinDeleteRequired, DELETE_STUDENT_TWINS, this::deleteTwinRecordsFromStudent)
        .step(DELETE_STUDENT_TWINS, STUDENT_TWINS_DELETED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private boolean isStudentTwinDeleteRequired(PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return ! isNotStudentTwinDeleteRequired(penRequestBatchUnmatchSagaData);
  }

  private boolean isNotStudentTwinDeleteRequired(PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return CollectionUtils.isEmpty(penRequestBatchUnmatchSagaData.getStudentTwinIDs());
  }

  /**
   * this method expects that the twin ids provided in the payload here is already validated.
   * Delete twin records to student.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUnmatchSagaData the pen request batch user actions saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void deleteTwinRecordsFromStudent(Event event, Saga saga, PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(DELETE_STUDENT_TWINS.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var studentTwinIDs = penRequestBatchUnmatchSagaData
        .getStudentTwinIDs();
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(DELETE_STUDENT_TWINS)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(JsonUtil.getJsonStringFromObject(studentTwinIDs))
                           .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for DELETE_STUDENT_TWINS Event.");
  }

  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event                              the event
   * @param penRequestBatchUnmatchSagaData the pen request batch user actions saga data
   * @return the pen request batch student
   */
  @Override
  protected PenRequestBatchStudent updateSagaDataAndCreatePRBStudent(Event event, PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUnmatchSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(FIXABLE.getCode());
    prbStudent.setAssignedPEN(null);
    prbStudent.setStudentID(null);
    prbStudent.setUpdateUser(penRequestBatchUnmatchSagaData.getUpdateUser());
    return prbStudent;
  }
}
