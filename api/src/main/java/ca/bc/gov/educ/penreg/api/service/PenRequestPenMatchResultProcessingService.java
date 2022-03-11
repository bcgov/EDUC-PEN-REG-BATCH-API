package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestPenMatchProcessingPayload;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import ca.bc.gov.educ.penreg.api.util.LocalIDUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * this is MyEd specific implementation.
 */
@Slf4j
@Service("penRequestPenMatchResultProcessingService")
public class PenRequestPenMatchResultProcessingService extends BasePenMatchResultProcessingService<PenRequestPenMatchProcessingPayload, Pair<Integer, Optional<PenRequestResult>>> {

  /**
   * The constant ALGORITHM.
   */
  public static final String ALGORITHM = "ALGORITHM";

  /**
   * The constant studentMapper.
   */

  public PenRequestPenMatchResultProcessingService(final RestUtils restUtils, final PenService penService) {
    super(restUtils, penService);
  }


  @Override
  public Pair<Integer, Optional<PenRequestResult>> processPenMatchResults(final PenRequestPenMatchProcessingPayload payload) {
    val penMatchResult = payload.getPenMatchResult();
    if (penMatchResult == null || penMatchResult.getPenStatus() == null) {
      log.error("Pen match result invalid :: {}", penMatchResult == null ? "null" : penMatchResult);
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    final var algorithmStatusCode = MatchAlgorithmStatusCode.valueOf(penMatchResult.getPenStatus());
    return super.handleBasedOnPenStatus(algorithmStatusCode, payload);
  }

  @Override
  public SchoolTypeCode getSchoolTypeCode() {
    return SchoolTypeCode.NONE;
  }


  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleDefault(final PenRequestPenMatchProcessingPayload payload) {
    return Pair.of(HttpStatus.MULTIPLE_CHOICES.value(), Optional.empty());
  }

  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleF1Status(final PenRequestPenMatchProcessingPayload payload) {
    return this.handleDefault(payload);
  }

  @SneakyThrows(JsonProcessingException.class)
  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleCreateNewStudentStatus(final PenRequestPenMatchProcessingPayload payload) {
    val penRequestResult = payload.getPenRequestResult();
    val pen = super.generateNewPen(UUID.randomUUID().toString());
    val student = PenRequestBatchMapper.mapper.toStudent(payload.getPenRequest(), pen);
    if (this.isGradeCodeWarningPresent(penRequestResult.getValidationIssues())) {
      student.setGradeCode(null);
    }
    val createStudentEvent = Event.builder().sagaId(payload.getTransactionID()).eventType(EventType.CREATE_STUDENT).eventPayload(JsonUtil.getJsonStringFromObject(student)).build();
    val createdStudent = this.getRestUtils().requestEventResponseFromStudentAPI(createStudentEvent);
    if (createdStudent.isPresent() && createdStudent.get().getEventOutcome() == EventOutcome.STUDENT_CREATED) {
      val optionalStudent = this.getRestUtils().getStudentByPEN(pen);
      if (optionalStudent.isEmpty()) {
        log.error("could not retrieve student data for new PEN status ");
        return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
      }
      val prr = PenRequestBatchMapper.mapper.toPenRequestResult(optionalStudent.get());
      prr.setValidationIssues(payload.getPenRequestResult().getValidationIssues());
      return Pair.of(HttpStatus.CREATED.value(), Optional.of(prr));
    }
    return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
  }

  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleSystemMatchedStatus(final PenRequestPenMatchProcessingPayload payload) {
    if (CollectionUtils.isEmpty(payload.getPenMatchResult().getMatchingRecords())) {
      log.error("Matching record should not be blank for sys matched status");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val firstStudent = payload.getPenMatchResult().getMatchingRecords().stream().findFirst();

    if (firstStudent.isEmpty() || StringUtils.isBlank(firstStudent.get().getMatchingPEN())) {
      log.error("Pen match result does not contain matched student for sys match status ");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val optionalStudent = this.getRestUtils().getStudentByPEN(firstStudent.get().getMatchingPEN());
    if (optionalStudent.isEmpty()) {
      log.error("could not retrieve student data for sys match status ");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    this.updateStudent(optionalStudent.get(), payload);
    val penRequestResult = PenRequestBatchMapper.mapper.toPenRequestResult(optionalStudent.get());
    penRequestResult.setValidationIssues(payload.getPenRequestResult().getValidationIssues());
    return Pair.of(HttpStatus.OK.value(), Optional.of(penRequestResult));
  }

  protected void updateStudent(final Student student, final PenRequestPenMatchProcessingPayload payload) {
    this.updateStudentData(student, payload);
    log.debug("Student payload for update :: {}", student);
    this.getRestUtils().updateStudent(student);
  }

  /**
   * This method updates the below.
   * Update the Student table for the matched Student record (Student ID above),
   * updating the values of the following fields, based on the values in the PEN Request Student and the PEN Request Batch record:
   * mincode (from the PEN Request Batch record)
   * Local ID
   * Student Grade Code
   * Postal Code
   * TBD: Do any of the other demographic values get updated? For K-12? For PSIs?
   *
   * @param studentFromStudentAPI          the student from student api
   * @param payload payload
   */
  protected void updateStudentData(final Student studentFromStudentAPI, final PenRequestPenMatchProcessingPayload payload) {
    studentFromStudentAPI.setMincode(payload.getPenRequest().getMincode());
    // updated as part of https://gww.jira.educ.gov.bc.ca/browse/PEN-1347
    final var changesBadLocalIDIfExistBeforeSetValue = LocalIDUtil.changeBadLocalID(StringUtils.remove(payload.getPenRequest().getLocalStudentID(), ' '));
    studentFromStudentAPI.setLocalID(changesBadLocalIDIfExistBeforeSetValue);
    this.updateGradeCodeAndGradeYear(studentFromStudentAPI, payload);

    if (StringUtils.isNotBlank(payload.getPenRequest().getPostalCode())) {
      studentFromStudentAPI.setPostalCode(payload.getPenRequest().getPostalCode());
    }

    this.updateUsualNameFields(studentFromStudentAPI, payload);

    studentFromStudentAPI.setHistoryActivityCode(StudentHistoryActivityCode.REQ_MATCH.getCode());
    studentFromStudentAPI.setUpdateUser(ALGORITHM);
  }

  /**
   * Update usual name fields.
   *
   * @param studentFromStudentAPI          the student from student api
   * @param payload the pen request batch student saga data
   */
  //Added as part of PEN-1007; Update the usual given & surnames if provided and not blank
  // updated as part of https://gww.jira.educ.gov.bc.ca/browse/PEN-1346
  protected void updateUsualNameFields(final Student studentFromStudentAPI, final PenRequestPenMatchProcessingPayload payload) {
    studentFromStudentAPI.setUsualFirstName(payload.getPenRequest().getUsualGivenName());
    studentFromStudentAPI.setUsualLastName(payload.getPenRequest().getUsualSurname());
    studentFromStudentAPI.setUsualMiddleNames(payload.getPenRequest().getUsualMiddleName());
  }

  /**
   * updated for https://gww.jira.educ.gov.bc.ca/browse/PEN-1348
   * When district number is <b> NOT </b> 102, apply the following logic for grade code & grade year.
   * If PEN Request grade code is null, and STUDENT record grade code is null do nothing
   * If PEN Request grade code has value, set it in the STUDENT record
   * Set the STUD_GRADE_YEAR to the current year (if after June 30) or the previous year (if before June 30)
   *
   * @param studentFromStudentAPI          the student from student api
   * @param payload the pen request batch student data
   */
  protected void updateGradeCodeAndGradeYear(final Student studentFromStudentAPI, final PenRequestPenMatchProcessingPayload payload) {
    if (!StringUtils.startsWith(payload.getPenRequest().getMincode(), "102")
      && !this.isGradeCodeWarningPresent(payload.getPenRequestResult().getValidationIssues())
      && ((StringUtils.isNotBlank(payload.getPenRequest().getEnrolledGradeCode()) && StringUtils.isNotBlank(studentFromStudentAPI.getGradeCode()))
      || (StringUtils.isNotBlank(payload.getPenRequest().getEnrolledGradeCode())))) {
      studentFromStudentAPI.setGradeCode(payload.getPenRequest().getEnrolledGradeCode());
      val localDateTime = LocalDateTime.now();
      if (localDateTime.getMonthValue() > 6) {
        studentFromStudentAPI.setGradeYear(String.valueOf(localDateTime.getYear()));
      } else {
        studentFromStudentAPI.setGradeYear(String.valueOf(localDateTime.getYear() - 1));
      }
    }
  }
}
