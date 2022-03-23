package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.PossibleMatch;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
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

  private final RestUtils restUtils;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   * @param restUtils
   */
  public PenReqBatchUserMatchOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RestUtils restUtils) {
    super(sagaService, messagePublisher, PenRequestBatchUserActionsSagaData.class,
      PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_TOPIC.toString());
    this.restUtils = restUtils;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(GET_STUDENT, this::getStudentByPen)
      .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::updateStudent)
      .step(UPDATE_STUDENT, STUDENT_UPDATED, this::isPossibleMatchAddRequired, ADD_POSSIBLE_MATCH, this::addPossibleMatchToStudent)
      .step(UPDATE_STUDENT, STUDENT_UPDATED, this::isPossibleMatchAddNotRequired, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
      .step(ADD_POSSIBLE_MATCH, POSSIBLE_MATCH_ADDED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
      .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
      .or()
      .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  private boolean isPossibleMatchAddRequired(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return !this.isPossibleMatchAddNotRequired(penRequestBatchUserActionsSagaData);
  }

  private boolean isPossibleMatchAddNotRequired(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
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
  protected void addPossibleMatchToStudent(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_POSSIBLE_MATCH.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final var possibleMatches = penRequestBatchUserActionsSagaData
      .getMatchedStudentIDList().stream()
      .map(matchedStudentID -> PossibleMatch.builder()
        .createUser(penRequestBatchUserActionsSagaData.getCreateUser())
        .updateUser(penRequestBatchUserActionsSagaData.getUpdateUser())
        .studentID(penRequestBatchUserActionsSagaData.getStudentID())
        .matchedStudentID(matchedStudentID)
        .matchReasonCode(TwinReasonCodes.PEN_MATCH.getCode())
        .build()).collect(Collectors.toList());
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(ADD_POSSIBLE_MATCH)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(possibleMatches))
      .build();
    this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for ADD_POSSIBLE_MATCH Event.");
  }

  /**
   * Gets student by pen.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   */
  protected void getStudentByPen(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(GET_STUDENT.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_STUDENT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(penRequestBatchUserActionsSagaData.getAssignedPEN())
      .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
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
  protected void updateStudent(final Event event, final Saga saga, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) throws JsonProcessingException {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_STUDENT.toString()); // set current event as saga state.
    final Student studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());
    studentDataFromEventResponse.setUpdateUser(penRequestBatchUserActionsSagaData.getUpdateUser());
    studentDataFromEventResponse.setMincode(penRequestBatchUserActionsSagaData.getMincode());
    studentDataFromEventResponse.setLocalID(penRequestBatchUserActionsSagaData.getLocalID());
    updateGradeCodeAndGradeYear(studentDataFromEventResponse, penRequestBatchUserActionsSagaData);
    updateUsualNameFields(studentDataFromEventResponse, penRequestBatchUserActionsSagaData);
    studentDataFromEventResponse.setPostalCode(penRequestBatchUserActionsSagaData.getPostalCode());
    studentDataFromEventResponse.setHistoryActivityCode(REQ_MATCH.getCode());
    penRequestBatchUserActionsSagaData.setStudentID(studentDataFromEventResponse.getStudentID());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_STUDENT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(studentDataFromEventResponse))
      .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
  }

  protected void updateUsualNameFields(final Student studentFromStudentAPI, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    studentFromStudentAPI.setUsualFirstName(penRequestBatchUserActionsSagaData.getUsualFirstName());
    studentFromStudentAPI.setUsualLastName(penRequestBatchUserActionsSagaData.getUsualLastName());
    studentFromStudentAPI.setUsualMiddleNames(penRequestBatchUserActionsSagaData.getUsualMiddleNames());
  }

  /**
   * updated for https://gww.jira.educ.gov.bc.ca/browse/PEN-1348
   * When district number is <b> NOT </b> 102, apply the following logic for grade code & grade year.
   * If the student record grade is null, and the incoming batch grade is valid, take it and update the grade year
   * Set the STUD_GRADE_YEAR to the current year (if after June 30) or the previous year (if before June 30)
   *
   * @param studentDataFromEventResponse          the student from student api
   * @param penRequestBatchUserActionsSagaData the pen request batch student data
   */
  protected void updateGradeCodeAndGradeYear(final Student studentDataFromEventResponse, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    final var gradeCodes = this.restUtils.getGradeCodes();
    var batchGradeCode = penRequestBatchUserActionsSagaData.getGradeCode();
    val isGradeCodeValid = StringUtils.isNotBlank(batchGradeCode) ? gradeCodes.stream().anyMatch(gradeCode1 -> LocalDateTime.now().isAfter(gradeCode1.getEffectiveDate())
      && LocalDateTime.now().isBefore(gradeCode1.getExpiryDate())
      && StringUtils.equalsIgnoreCase(batchGradeCode, gradeCode1.getGradeCode())) : false;

    if (isGradeCodeValid && StringUtils.isBlank(studentDataFromEventResponse.getGradeCode())) {
      studentDataFromEventResponse.setGradeCode(batchGradeCode);
      val localDateTime = LocalDateTime.now();
      if (localDateTime.getMonthValue() > 6) {
        studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear()));
      } else {
        studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear() - 1));
      }
    }
  }

  /**
   * check if there is a warning present in the validation.
   *
   * @param validationIssueEntities the validation issues
   * @return the boolean true if validation of grade code resulted in warning else false.
   */
  protected boolean isGradeCodeWarningPresent(final List<PenRequestValidationIssue> validationIssueEntities) {
    return validationIssueEntities.stream().anyMatch(entity -> "GRADECODE".equals(entity.getPenRequestBatchValidationFieldCode()));
  }

  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event                              the event
   * @param penRequestBatchUserActionsSagaData the pen request batch user actions saga data
   * @return the pen request batch student
   */
  @Override
  protected PenRequestBatchStudent createPRBStudent(final Event event, final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    final var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchUserActionsSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(USR_MATCHED.getCode());
    prbStudent.setAssignedPEN(penRequestBatchUserActionsSagaData.getAssignedPEN());
    prbStudent.setStudentID(penRequestBatchUserActionsSagaData.getStudentID());
    prbStudent.setUpdateUser(penRequestBatchUserActionsSagaData.getUpdateUser());
    return prbStudent;
  }
}
