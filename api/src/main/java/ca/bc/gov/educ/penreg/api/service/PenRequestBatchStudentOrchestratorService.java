package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.service.interfaces.PenMatchResultProcessingService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.BatchStudentPenMatchProcessingPayload;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch student orchestrator service.
 */
@Service
@Slf4j
public class PenRequestBatchStudentOrchestratorService {


  /**
   * The Pen request batch service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentService penRequestBatchStudentService;


  /**
   * The Pen service.
   */
  @Getter(PRIVATE)
  private final PenService penService;


  /**
   * The Processing service.
   */
  private final PenMatchResultProcessingService<BatchStudentPenMatchProcessingPayload, Optional<Event>> processingService;

  /**
   * Instantiates a new Pen request batch student orchestrator service.
   *
   * @param penRequestBatchStudentService the pen request batch student service
   * @param penService                    the pen service
   * @param processingService             the processing service
   */
  public PenRequestBatchStudentOrchestratorService(final PenRequestBatchStudentService penRequestBatchStudentService, final PenService penService,
                                                   @Qualifier("penRequestBatchStudentPenMatchResultProcessingService") final PenMatchResultProcessingService<BatchStudentPenMatchProcessingPayload, Optional<Event>> processingService) {
    this.penRequestBatchStudentService = penRequestBatchStudentService;
    this.penService = penService;
    this.processingService = processingService;
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
          var isRecordAlreadyPresent = false;
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

  /**
   * Add validation issue entities to student.
   *
   * @param validationIssueEntities the validation issue entities
   * @param student                 the student
   */
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
    log.debug("Payload before scrubbing :: {}", updatedPayload);
    if (StringUtils.isNotBlank(sagaData.getLegalLastName())) {
      updatedPayload.setLegalLastName(this.scrubNameField(sagaData.getLegalLastName()));
    }
    if (StringUtils.isNotBlank(sagaData.getLegalFirstName())) {
      updatedPayload.setLegalFirstName(this.scrubNameField(sagaData.getLegalFirstName()));
    }
    if (StringUtils.isNotBlank(sagaData.getLegalMiddleNames())) {
      updatedPayload.setLegalMiddleNames(this.scrubNameField(sagaData.getLegalMiddleNames()));
    }
    if (StringUtils.isNotBlank(updatedPayload.getUsualFirstName())) {
      updatedPayload.setUsualFirstName(this.scrubNameField(updatedPayload.getUsualFirstName()));
    }
    if (StringUtils.isNotBlank(updatedPayload.getUsualMiddleNames())) {
      updatedPayload.setUsualMiddleNames(this.scrubNameField(updatedPayload.getUsualMiddleNames()));
    }
    if (StringUtils.isNotBlank(updatedPayload.getUsualLastName())) {
      updatedPayload.setUsualLastName(this.scrubNameField(updatedPayload.getUsualLastName()));
    }
    final var usualFirstName = sagaData.getUsualFirstName();
    if (StringUtils.isNotEmpty(usualFirstName)
      && (StringUtils.length(StringUtils.trim(usualFirstName)) == 1 || StringUtils.equals(usualFirstName, sagaData.getLegalFirstName()))) {
      updatedPayload.setUsualFirstName(null);
    }

    final var usualLastName = sagaData.getUsualLastName();
    if (StringUtils.isNotEmpty(usualLastName)
      && (StringUtils.length(StringUtils.trim(usualLastName)) == 1 || StringUtils.equals(usualLastName, sagaData.getLegalLastName()))) {
      updatedPayload.setUsualLastName(null);
    }
    final var usualMiddleName = sagaData.getUsualMiddleNames();
    if (this.doesMiddleNameNeedsToBeBlank(usualMiddleName, sagaData)) {
      updatedPayload.setUsualMiddleNames(null);
    }
    log.debug("Payload after scrubbing :: {}", updatedPayload);
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
      || this.areBothFieldValueEqual(usualMiddleName, sagaData.getLegalFirstName())
      || this.areBothFieldValueEqual(usualMiddleName, sagaData.getUsualFirstName())
      || (usualMiddleName.trim().equals(sagaData.getLegalFirstName() + " " + sagaData.getLegalMiddleNames()))
      || (StringUtils.isNotBlank(sagaData.getLegalMiddleNames()) && sagaData.getLegalMiddleNames().contains(usualMiddleName)));
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

  /**
   * Process pen match result optional.
   *
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @param penMatchResult                 the pen match result
   * @return the optional
   */
  public Optional<Event> processPenMatchResult(final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData, final PenMatchResult penMatchResult) {
    return this.processingService.processPenMatchResults(BatchStudentPenMatchProcessingPayload.builder().penMatchResult(penMatchResult).penRequestBatchStudentSagaData(penRequestBatchStudentSagaData).saga(saga).build());
  }
}
