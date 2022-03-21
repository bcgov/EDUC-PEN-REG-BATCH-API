package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.struct.v1.StudentHistory;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.DELETE_POSSIBLE_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GET_STUDENT_HISTORY;
import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.FIXABLE;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;

/**
 * The type Pen req batch user unmatch orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchUserUnmatchOrchestrator extends BaseUserActionsOrchestrator<PenRequestBatchUnmatchSagaData> {

  private final RestUtils restUtils;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   */
  public PenReqBatchUserUnmatchOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RestUtils restUtils) {
    super(sagaService, messagePublisher, PenRequestBatchUnmatchSagaData.class,
        PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_TOPIC.toString());

    this.restUtils = restUtils;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
        .begin(this::isPossibleMatchDeleteNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .or()
        .begin(this::isPossibleMatchDeleteRequired, DELETE_POSSIBLE_MATCH, this::deletePossibleMatchesFromStudent)
        .step(DELETE_POSSIBLE_MATCH, POSSIBLE_MATCH_DELETED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED, GET_STUDENT_HISTORY, this::readAuditHistory)
        .step(GET_STUDENT_HISTORY, STUDENT_HISTORY_FOUND, UPDATE_STUDENT, this::revertStudentInformation)
        .end(UPDATE_STUDENT, STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  /**
   * Obtaining student history records.
   *
   * @param event                        the event
   * @param saga                         the saga
   * @param penRequestBatchUnmatchSagaData the pen request batch user actions unmatch saga data
   */
  protected void readAuditHistory(final Event event, final Saga saga, final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_STUDENT_HISTORY.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(GET_STUDENT_HISTORY)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(penRequestBatchUnmatchSagaData.getStudentID())
        .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT_HISTORY Event.");
  }

  /**
   * This function will revert the student information to the previous state in history before the latest REQ_MATCH in the following steps:
   * 1) It will sort the student's Audit History in DESC order.
   * 2) It will find the record right before REQ_MATCH
   * 3) It will then revert the student record by updating it to the record right before REQ_MATCH
   *
   * the following attributes on the unmatched student record will be updated
   * First usual name
   * Middle usual name
   * Last usual name
   * Mincode
   * Local ID
   * Student Grade Code
   * Grade Year
   * Postal Code
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUnmatchSagaData the pen request batch user actions unmatch saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void revertStudentInformation(final Event event, final Saga saga, final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) throws JsonProcessingException {
    StudentHistory studentHistoryForRevert = new StudentHistory();

    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_STUDENT.toString()); // set current event as saga state.

    //convert payload to StudentHistory List
    final ObjectMapper objectMapper = new ObjectMapper();
    final JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, StudentHistory.class);
    final List<StudentHistory> historyList = objectMapper.readValue(event.getEventPayload(), type);

    //sort list in DESC order by CreateDate
    Collections.sort(historyList, Collections.reverseOrder(Comparator.comparing(StudentHistory::getCreateDate)));

    //find the most recent student history record before the REQ MATCH.
    for (int i=0;i<historyList.size(); i++) {
      if (StringUtils.equals(historyList.get(i).getHistoryActivityCode(), StudentHistoryActivityCode.REQ_MATCH.getCode())) {
        studentHistoryForRevert = historyList.get(i+1);
        log.debug("reverting student with this student audit history record ::{}", studentHistoryForRevert);
        break;
      }
    }

    //grab the student's most recent record to update.
    final Student studentInformation = this.restUtils.getStudentByStudentID(penRequestBatchUnmatchSagaData.getStudentID());

    studentInformation.setUpdateUser(penRequestBatchUnmatchSagaData.getUpdateUser());
    studentInformation.setUsualFirstName(studentHistoryForRevert.getUsualFirstName());
    studentInformation.setUsualMiddleNames(studentHistoryForRevert.getUsualMiddleNames());
    studentInformation.setUsualLastName(studentHistoryForRevert.getUsualLastName());
    studentInformation.setMincode(studentHistoryForRevert.getMincode());
    studentInformation.setLocalID(studentHistoryForRevert.getLocalID());
    studentInformation.setGradeCode(studentHistoryForRevert.getGradeCode());
    studentInformation.setGradeYear(studentHistoryForRevert.getGradeYear());
    studentInformation.setPostalCode(studentHistoryForRevert.getPostalCode());

    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(UPDATE_STUDENT)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentInformation))
        .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
  }

  private boolean isPossibleMatchDeleteRequired(final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return !this.isPossibleMatchDeleteNotRequired(penRequestBatchUnmatchSagaData);
  }

  private boolean isPossibleMatchDeleteNotRequired(final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
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
  protected void deletePossibleMatchesFromStudent(final Event event, final Saga saga, final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) throws JsonProcessingException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(DELETE_POSSIBLE_MATCH.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final List<PossibleMatch> possibleMatches = new ArrayList<>();
    penRequestBatchUnmatchSagaData
        .getMatchedStudentIDList().forEach(element -> {
      final PossibleMatch possibleMatch = new PossibleMatch();
      possibleMatch.setStudentID(penRequestBatchUnmatchSagaData.getStudentID());
      possibleMatch.setMatchedStudentID(element);
      possibleMatch.setCreateUser(penRequestBatchUnmatchSagaData.getCreateUser());
      possibleMatch.setUpdateUser(penRequestBatchUnmatchSagaData.getUpdateUser());
      possibleMatches.add(possibleMatch);
    });
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(DELETE_POSSIBLE_MATCH)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(possibleMatches))
        .build();
    this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
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
  protected PenRequestBatchStudent createPRBStudent(final Event event, final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    final var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUnmatchSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(FIXABLE.getCode());
    prbStudent.setAssignedPEN(null);
    prbStudent.setStudentID(null);
    prbStudent.setUpdateUser(penRequestBatchUnmatchSagaData.getUpdateUser());
    return prbStudent;
  }
}
