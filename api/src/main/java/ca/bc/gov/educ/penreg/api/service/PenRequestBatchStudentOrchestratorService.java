package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.MatchAlgorithmStatusCode;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.PEN_MATCH_RESULTS_PROCESSED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.PROCESS_PEN_MATCH_RESULTS;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.*;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_NEW;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch student orchestrator service.
 */
@Service
@Slf4j
public class PenRequestBatchStudentOrchestratorService {
  /**
   * The constant studentMapper.
   */
  private static final StudentMapper studentMapper = StudentMapper.mapper;
  public static final String ALGORITHM = "ALGORITHM";
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
   * The Rest utils.
   */
  @Getter(PRIVATE)
  private final RestUtils restUtils;


  /**
   * The Pen service.
   */
  @Getter(PRIVATE)
  private final PenService penService;
  /**
   * The Saga service.
   */
  @Getter(PRIVATE)
  private final SagaService sagaService;

  /**
   * Instantiates a new Pen request batch student orchestrator service.
   *
   * @param penRequestBatchService        the pen request batch service
   * @param penRequestBatchStudentService the pen request batch student service
   * @param restUtils                     the rest utils
   * @param penService                    the pen service
   * @param sagaService                   the saga service
   */
  public PenRequestBatchStudentOrchestratorService(final PenRequestBatchService penRequestBatchService, final PenRequestBatchStudentService penRequestBatchStudentService, final RestUtils restUtils, final PenService penService, final SagaService sagaService) {
    this.penRequestBatchService = penRequestBatchService;
    this.penRequestBatchStudentService = penRequestBatchStudentService;
    this.restUtils = restUtils;
    this.penService = penService;
    this.sagaService = sagaService;
  }

  /**
   * Process pen match result optional.
   * Process pen match results.
   * * this will go through and process the result based on the logic provided here.
   * * <pre>
   *    *    Update the PEN Request Student record with the outcome of the PEN Match process. Set the values of these columns:
   *    *        Match Algorithm Status Code
   *    *        Questionable Match Student ID
   *    *    IF the outcome is that the request is matched to an existing Student record: D1
   *    *        Update the PEN Request Student Status Code on the PEN Request Student record to: MATCHEDSYS
   *    *        Update the Student ID foreign key on the PEN Request Student record to specify the matched Student record.
   *    *        Update the Student table for the matched Student record (Student ID above), updating the values of the following fields, based on the values in the PEN Request Student and the PEN Request Batch record:
   *    *        mincode (from the PEN Request Batch record)
   *    *        Local ID
   *    *        Student Grade Code
   *    *        Postal Code
   *    *        TBD: Do any of the other demographic values get updated? For K-12? For PSIs?
   *    *    ELSEIF the outcome is that a new Student/PEN record is created to fulfill the request: B0, C0, D0
   *    *        Insert a new record in the Student table based on the data from the PEN Request Student record (and mincode from the PEN Request Batch record). This will have a new Student ID. Populate the new Student record with these values:
   *    *        These attributes come from the PEN Request Batch header:
   *    *          mincode
   *    *          Memo = 'Generated by the system, as part of submission <Submission#>', where <Submission#> is the Submission No from the batch header
   *    *          These attributes come from the PEN Request Student detail:
   *    *          Local ID, Legal First Name, Legal Middle Names, Legal Last Name, DOB, Gender Code, Usual First Name, Usual Middle Names, Usual Last Name, Postal Code, and Student Grade Code
   *    *          These other attributes shall be:
   *    *          PEN - Generate the next new PEN number. This consists of the next 8-digit value (one more than the current largest one, in the current range of PENs who's first digit is a "1"; do not consider legacy PEN ranges whose first digit is other than a 1). Then calculate the check digit for these 8 digits, and append it as the 9th digit. This is the new PEN. See PEN Number Check-Digit Algorithm
   *    *          Email Verified = N
   *    *          Deceased Date = null
   *    *          Grade Year = null
   *    *          Demog Code = A
   *    *          Status Code = A
   *    *          Create Date, Update Date = current datetime
   *    *          Create_User, Update User = PENMATCH
   *    *          Update the Student ID foreign key on the PEN Request Student record to specify the new Student record just created.
   *    *          Update the PEN Request Student Status Code on the PEN Request Student record to: NEWPENSYS
   *    *   ELSEIF the outcome is uncertain, such that (Submitted PEN Status Code = F1 OR PEN Match Phase 1 Status Code = F1):
   *    *          Update the value of Questionable Match Student ID on the PEN Request Student record.
   *    *          Out of Scope: Run phase 2 (New Match) of the PEN match algorithm. The status on the request will remain as LOADED.
   *    *   ELSE the request requires manual review:
   *    *      Update the PEN Request Student Status Code on the PEN Request Student record to: FIXABLE
   *    *
   *    * </pre>
   *
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @param penMatchResult                 the pen match result
   * @return the optional
   */
  public Optional<Event> processPenMatchResult(final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData, final PenMatchResult penMatchResult) throws JsonProcessingException {
    final var algorithmStatusCode = MatchAlgorithmStatusCode.valueOf(penMatchResult.getPenStatus());
    final var penRequestBatchStudent = this.getPenRequestBatchStudentService()
        .getStudentById(penRequestBatchStudentSagaData.getPenRequestBatchID(), penRequestBatchStudentSagaData.getPenRequestBatchStudentID());

    final var penRequestBatch = penRequestBatchStudent.getPenRequestBatchEntity();
    penRequestBatchStudent.setMatchAlgorithmStatusCode(algorithmStatusCode.toString());
    final Optional<Event> eventOptional;
    switch (algorithmStatusCode) {
      case AA:
      case B1:
      case C1:
      case D1:
        eventOptional = Optional.of(this.handleSystemMatchedStatus(saga, penMatchResult, penRequestBatchStudent, penRequestBatch));
        break;
      case B0:
      case C0:
      case D0:
        eventOptional = Optional.of(this.handleCreateNewStudentStatus(saga, penRequestBatchStudentSagaData, penRequestBatchStudent, penMatchResult));
        break;
      case F1:
        eventOptional = Optional.of(this.handleF1Status(saga, penMatchResult, penRequestBatchStudent)); // FIXABLE
        break;
      case BM:
      case CM:
      case DM:
      case G0:
      default:
        eventOptional = Optional.of(this.handleDefault(saga, penRequestBatchStudent, penMatchResult));
        break;
    }
    return eventOptional;
  }

  /**
   * Handle f 1 status event.
   *
   * @param saga                   the saga
   * @param penMatchResult         the pen match result
   * @param penRequestBatchStudent the pen request batch student
   * @return the event
   */
  private Event handleF1Status(final Saga saga, final PenMatchResult penMatchResult, final PenRequestBatchStudentEntity penRequestBatchStudent) {
    final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
    penMatchRecordOptional.ifPresent(penMatchRecord -> penRequestBatchStudent.setQuestionableMatchStudentId(UUID.fromString(penMatchRecord.getStudentID())));
    penRequestBatchStudent.setPenRequestBatchStudentStatusCode(FIXABLE.getCode());
    this.getPenRequestBatchStudentService().saveAttachedEntity(penRequestBatchStudent);
    return Event.builder().sagaId(saga.getSagaId())
        .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
        .eventPayload(penMatchResult.getPenStatus()).build();
  }

  /**
   * Handle default.
   *
   * @param saga                   the saga
   * @param penRequestBatchStudent the pen request batch student
   * @param penMatchResult         the pen match result
   * @return the event
   */
  private Event handleDefault(final Saga saga, final PenRequestBatchStudentEntity penRequestBatchStudent, final PenMatchResult penMatchResult) {
    penRequestBatchStudent.setPenRequestBatchStudentStatusCode(FIXABLE.getCode());
    this.getPenRequestBatchStudentService().saveAttachedEntity(penRequestBatchStudent);
    return Event.builder().sagaId(saga.getSagaId())
        .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
        .eventPayload(penMatchResult.getPenStatus()).build();
  }

  /**
   * Handle create new student status.
   *
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @param penRequestBatchStudent         the pen request batch student
   * @param penMatchResult                 the pen match result
   * @return the event
   */
  private Event handleCreateNewStudentStatus(final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData, final PenRequestBatchStudentEntity penRequestBatchStudent, final PenMatchResult penMatchResult) throws JsonProcessingException {
    final String pen;
    if (penRequestBatchStudent.getStudentID() == null) {
      if (StringUtils.isBlank(penRequestBatchStudentSagaData.getGeneratedPEN())) {
        pen = this.generateNewPen(saga.getSagaId().toString());
        penRequestBatchStudentSagaData.setGeneratedPEN(pen); // store it in payload, will be used in case of replay.
        saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData));
        this.getSagaService().updateAttachedEntityDuringSagaProcess(saga); // update the payload withe generated PEN and  save the updated payload to DB...
      } else {
        pen = penRequestBatchStudentSagaData.getGeneratedPEN();
      }
      final var student = studentMapper.toStudent(penRequestBatchStudentSagaData);
      student.setPen(pen);
      student.setHistoryActivityCode(REQ_NEW.getCode());
      student.setCreateUser(ALGORITHM);
      student.setUpdateUser(ALGORITHM);
      penRequestBatchStudent.setAssignedPEN(pen);
      final var studentFromStudentAPIOptional = this.getRestUtils().getStudentByPEN(pen);
      if (studentFromStudentAPIOptional.isEmpty()) { // create the student only if it does not exist.
        final var studentFromAPIResponse = this.getRestUtils().createStudent(student);
        penRequestBatchStudent.setStudentID(UUID.fromString(studentFromAPIResponse.getStudentID()));
      } else {
        penRequestBatchStudent.setStudentID(UUID.fromString(studentFromStudentAPIOptional.get().getStudentID()));
      }
      penRequestBatchStudent.setPenRequestBatchStudentStatusCode(SYS_NEW_PEN.getCode());
      this.getPenRequestBatchStudentService().saveAttachedEntity(penRequestBatchStudent);
    } else {
      log.info("Student ID is already present for PRBStudent, replay process..., student ID :: {} , PRBStudent ID:: {}", penRequestBatchStudent.getStudentID(), penRequestBatchStudent.getPenRequestBatchStudentID());
    }

    return Event.builder().sagaId(saga.getSagaId())
        .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
        .eventPayload(penMatchResult.getPenStatus()).build();
  }

  /**
   *       case AA:
   *       case B1:
   *       case C1:
   *       case D1:
   *
   * @param saga                   the saga
   * @param penMatchResult         the pen match result
   * @param penRequestBatchStudent the pen request batch student
   * @param penRequestBatch        the pen request batch
   * @return the event
   */
  private Event handleSystemMatchedStatus(final Saga saga, final PenMatchResult penMatchResult, PenRequestBatchStudentEntity penRequestBatchStudent, final PenRequestBatchEntity penRequestBatch) {
    final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
    if (penMatchRecordOptional.isPresent()) {
      final var penMatchRecord = penMatchRecordOptional.get();
      final var studentID = penMatchRecord.getStudentID();
      penRequestBatchStudent.setPenRequestBatchStudentStatusCode(SYS_MATCHED.getCode());
      penRequestBatchStudent.setStudentID(UUID.fromString(studentID));
      penRequestBatchStudent.setAssignedPEN(penMatchRecord.getMatchingPEN());
      penRequestBatchStudent = this.getPenRequestBatchStudentService().saveAttachedEntity(penRequestBatchStudent);
      final var studentFromStudentAPI = this.getRestUtils().getStudentByStudentID(studentID);
      this.updateStudentData(studentFromStudentAPI, penRequestBatchStudent, penRequestBatch);
      this.getRestUtils().updateStudent(studentFromStudentAPI);
      return Event.builder().sagaId(saga.getSagaId())
          .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
          .eventPayload(penMatchResult.getPenStatus()).build();
    } else {
      log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      throw new PenRegAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
    }
  }


  /**
   * Generate new pen string.
   *
   * @param guid the guid to identify the transaction.
   * @return the string
   */
  private String generateNewPen(final String guid) {
    log.info("generate new pen called for guid :: {}", guid);
    final String pen = this.getPenService().getNextPenNumber(guid);
    log.info("got new pen :: {} for guid :: {}", pen, guid);
    return pen;
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
   * @param studentFromStudentAPI  the student from student api
   * @param penRequestBatchStudent the pen request batch student
   * @param penRequestBatchEntity  the pen request batch entity
   */
  private void updateStudentData(final Student studentFromStudentAPI, final PenRequestBatchStudentEntity penRequestBatchStudent, final PenRequestBatchEntity penRequestBatchEntity) {
    studentFromStudentAPI.setMincode(penRequestBatchEntity.getMincode());
    studentFromStudentAPI.setLocalID(penRequestBatchStudent.getLocalID());
    studentFromStudentAPI.setGradeCode(penRequestBatchStudent.getGradeCode());
    studentFromStudentAPI.setGradeYear(Integer.toString(LocalDateTime.now().getYear()));
    studentFromStudentAPI.setPostalCode(penRequestBatchStudent.getPostalCode());

    //Added as part of PEN-1007; Update the usual given & surnames if provided and not blank
    if (StringUtils.isNotBlank(penRequestBatchStudent.getUsualFirstName())) {
      studentFromStudentAPI.setUsualFirstName(penRequestBatchStudent.getUsualFirstName());
    }

    if(StringUtils.isNotBlank(penRequestBatchStudent.getUsualLastName())){
      studentFromStudentAPI.setUsualLastName(penRequestBatchStudent.getUsualLastName());
    }

    studentFromStudentAPI.setHistoryActivityCode(StudentHistoryActivityCode.REQ_MATCH.getCode());
    studentFromStudentAPI.setUpdateUser(ALGORITHM);
  }

  /**
   * Save demog validation results and update student status.
   *
   * @param validationIssueEntities  the validation issue entities
   * @param statusCode               the status code
   * @param penRequestBatchStudentID the pen request batch student id
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveDemogValidationResultsAndUpdateStudentStatus(final List<PenRequestBatchStudentValidationIssueEntity> validationIssueEntities, final PenRequestBatchStudentStatusCodes statusCode, final UUID penRequestBatchStudentID) {
    final var studentOptional = this.getPenRequestBatchStudentService().findByID(penRequestBatchStudentID);
    if (studentOptional.isPresent()) {
      final var student = studentOptional.get();
      student.setPenRequestBatchStudentStatusCode(statusCode.getCode());
      student.setUpdateDate(LocalDateTime.now());
      student.setUpdateUser("PEN_REQUEST_BATCH_API");
      log.info("current validation issue entities size {} for Student :: {}", student.getPenRequestBatchStudentValidationIssueEntities().size(), student.getPenRequestBatchStudentID());
      if (!student.getPenRequestBatchStudentValidationIssueEntities().isEmpty()) {
        final var filteredIssues = validationIssueEntities.stream().filter(el -> {
          boolean isRecordAlreadyPresent = false;
          for (final var validationIssue : student.getPenRequestBatchStudentValidationIssueEntities()) {
            if (StringUtils.equalsIgnoreCase(validationIssue.getPenRequestBatchValidationFieldCode(), el.getPenRequestBatchValidationFieldCode())
                && StringUtils.equalsIgnoreCase(validationIssue.getPenRequestBatchValidationIssueSeverityCode(), el.getPenRequestBatchValidationIssueSeverityCode())
                && StringUtils.equalsIgnoreCase(validationIssue.getPenRequestBatchValidationIssueTypeCode(), el.getPenRequestBatchValidationIssueTypeCode())) {
              isRecordAlreadyPresent = true;
              break;
            }
          }
          return !isRecordAlreadyPresent;
        }).collect(Collectors.toSet());
        this.addValidationIssueEntitiesToStudent(filteredIssues, student);
      } else {
        this.addValidationIssueEntitiesToStudent(validationIssueEntities, student);
      }
      this.getPenRequestBatchStudentService().saveAttachedEntityWithChildEntities(student);

    } else {
      log.error("Student request record could not be found for :: {}", penRequestBatchStudentID);
    }
  }

  private void addValidationIssueEntitiesToStudent(final Collection<PenRequestBatchStudentValidationIssueEntity> validationIssueEntities, final PenRequestBatchStudentEntity student) {
    for (final var issue : validationIssueEntities) {
      issue.setPenRequestBatchStudentEntity(student);
      student.getPenRequestBatchStudentValidationIssueEntities().add(issue);
    }
  }

  /**
   * the method makes a deep clone as it needs the original sagaData to do comparison and update fields.
   * Perform the following actions on the CurrentRequest:
   * <p>
   * Remove periods from all legal and usual names elements
   * Convert all name elements to all UPPER case.
   * Change tab characters within name elements to single spaces
   * Remove leading and trailing spaces from all name elements
   * Convert multiple contiguous spaces within any name element to a single space
   * Blank out usual name if it is an initial (a single character)
   * Blank out each of the three usual name elements if it is the same as the corresponding legal name element
   * Blank out usual middle name if it is the same as legal given name
   * Blank out usual middle name if it is the same as usual given name
   * Blank out usual middle name if it is the same as (legal given name + space + legal middle name)
   * Blank out usual middle name if it is contained within legal middle name
   *
   * @param sagaData the sagaData which needs to be updated.
   * @return the updated sagaData.
   * @throws JsonProcessingException the json processing exception
   */
  public PenRequestBatchStudentSagaData scrubPayload(final PenRequestBatchStudentSagaData sagaData) throws JsonProcessingException {
    final PenRequestBatchStudentSagaData updatedPayload = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, JsonUtil.getJsonStringFromObject(sagaData));
    if (StringUtils.isNotBlank(sagaData.getLegalLastName())) {
      updatedPayload.setLegalLastName(this.scrubNameField(sagaData.getLegalLastName()));
    }
    if (StringUtils.isNotBlank(sagaData.getLegalFirstName())) {
      updatedPayload.setLegalFirstName(this.scrubNameField(sagaData.getLegalFirstName()));
    }
    if (StringUtils.isNotBlank(sagaData.getLegalMiddleNames())) {
      updatedPayload.setLegalMiddleNames(this.scrubNameField(sagaData.getLegalMiddleNames()));
    }
    final var usualFirstName = sagaData.getUsualFirstName();
    if (StringUtils.isNotEmpty(usualFirstName)
        && (StringUtils.length(StringUtils.trim(usualFirstName)) == 1 || StringUtils.equals(usualFirstName, sagaData.getLegalFirstName()))) {
      updatedPayload.setUsualFirstName("");
    }

    final var usualLastName = sagaData.getUsualLastName();
    if (StringUtils.isNotEmpty(usualLastName)
        && (StringUtils.length(StringUtils.trim(usualLastName)) == 1 || StringUtils.equals(usualLastName, sagaData.getLegalLastName()))) {
      updatedPayload.setUsualLastName("");
    }
    final var usualMiddleName = sagaData.getUsualMiddleNames();
    if (this.doesMiddleNameNeedsToBeBlank(usualMiddleName, sagaData)) {
      updatedPayload.setUsualMiddleNames("");
    }

    return updatedPayload;
  }

  /**
   * Blank out usual middle name if it is the same as legal given name
   * Blank out usual middle name if it is the same as usual given name
   * Blank out usual middle name if it is the same as (legal given name + space + legal middle name)
   * Blank out usual middle name if it is contained within legal middle name
   *
   * @param usualMiddleName the usual middle name
   * @param sagaData        the sagaData
   * @return boolean boolean
   */
  private boolean doesMiddleNameNeedsToBeBlank(final String usualMiddleName, final PenRequestBatchStudentSagaData sagaData) {
    return StringUtils.isNotEmpty(usualMiddleName)
        && (usualMiddleName.trim().length() == 1
        || this.areBothFieldValueEqual(usualMiddleName, sagaData.getLegalMiddleNames())
        || this.areBothFieldValueEqual(usualMiddleName, sagaData.getLegalFirstName())
        || (StringUtils.isNotBlank(sagaData.getLegalMiddleNames()) && sagaData.getLegalMiddleNames().contains(usualMiddleName))
        || (usualMiddleName.trim().equals(sagaData.getLegalFirstName() + " " + sagaData.getLegalMiddleNames())));
  }

  /**
   * Are both field value equal boolean.
   *
   * @param field1 the field 1
   * @param field2 the field 2
   * @return the boolean
   */
  protected boolean areBothFieldValueEqual(final String field1, final String field2) {
    return StringUtils.equals(StringUtils.trim(field1), StringUtils.trim(field2));
  }

  /**
   * This method is responsible to do the following.
   * 1.Remove periods from all legal and usual names elements
   * 2.Convert all name elements to all UPPER case.
   * 3.Change tab characters within name elements to single spaces
   * 4.Remove leading and trailing spaces from all name elements
   * 5.Convert multiple contiguous spaces within any name element to a single space
   *
   * @param nameFieldValue the value of the name field
   * @return modified string value.
   */
  protected String scrubNameField(final String nameFieldValue) {
    return nameFieldValue.trim().toUpperCase().replace("\t", " ").replace(".", "").replaceAll("\\s{2,}", " ");
  }
}
