package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.DELETE_POSSIBLE_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_TOPIC;

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
        .begin(this::isPossibleMatchDeleteNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .or()
        .begin(this::isPossibleMatchDeleteRequired, DELETE_POSSIBLE_MATCH, this::deletePossibleMatchesFromStudent)
        .step(DELETE_POSSIBLE_MATCH, POSSIBLE_MATCH_DELETED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private boolean isPossibleMatchDeleteRequired(PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return !isPossibleMatchDeleteNotRequired(penRequestBatchUnmatchSagaData);
  }

  private boolean isPossibleMatchDeleteNotRequired(PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return CollectionUtils.isEmpty(penRequestBatchUnmatchSagaData.getMatchedStudentIDList());
  }

  /**
   * this method expects that the twin ids provided in the payload here is already validated.
   * Delete twin records to student.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchUnmatchSagaData the pen request batch user actions saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void deletePossibleMatchesFromStudent(Event event, Saga saga, PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(DELETE_POSSIBLE_MATCH.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final List<Map<String, UUID>> payload = new ArrayList<>();
    penRequestBatchUnmatchSagaData
        .getMatchedStudentIDList().forEach(element -> {
      final Map<String, UUID> deletePossibleMatchMap = new HashMap<>();
      deletePossibleMatchMap.put("studentID", UUID.fromString(penRequestBatchUnmatchSagaData.getStudentID()));
      deletePossibleMatchMap.put("matchedStudentID", UUID.fromString(element));
      payload.add(deletePossibleMatchMap);
    });
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(DELETE_POSSIBLE_MATCH)
        .replyTo(getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(payload))
        .build();
    postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for DELETE_POSSIBLE_MATCH Event.");
  }

  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event                          the event
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
