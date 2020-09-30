package ca.bc.gov.educ.penreg.api.validation.rules.impl;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.validation.rules.BaseRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationFieldCode.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueSeverityCode.WARNING;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueTypeCode.EMBEDDED_MID;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueTypeCode.REPEAT_MID;

/**
 * The type Legal middle name rule.
 */
@Slf4j
public class LegalMiddleNameRule extends BaseRule {
  /**
   * Validates the student record for the given rule.
   *
   * @param validationPayload the validation payload
   * @return the validation result as a list.
   */
  @Override
  public List<PenRequestBatchStudentValidationIssue> validate(PenRequestBatchStudentValidationPayload validationPayload) {
    final List<PenRequestBatchStudentValidationIssue> results = new LinkedList<>();
    var legalMiddleName = validationPayload.getLegalMiddleNames();
    if (StringUtils.isNotBlank(legalMiddleName)) {
      legalMiddleName = legalMiddleName.trim();
      defaultValidationForNameFields(results, legalMiddleName, LEGAL_MID);
    }
    if (results.isEmpty() && StringUtils.isNotBlank(legalMiddleName)) {
      checkFieldValueExactMatchWithInvalidText(results, legalMiddleName, LEGAL_MID, validationPayload.getIsInteractive());
    }
    if (results.isEmpty() && StringUtils.isNotBlank(legalMiddleName)
        && legalFirstNameHasNoErrors(validationPayload) && legalLastNameHasNoErrors(validationPayload)
        && (legalMiddleName.equals(validationPayload.getLegalFirstName()) || legalMiddleName.equals(validationPayload.getLegalLastName()))) {
      results.add(createValidationEntity(WARNING, REPEAT_MID, LEGAL_MID));
    } else {
      log.debug("Legal First Name and Legal Last Name has errors so, skipping this check :: {}", validationPayload.getTransactionID());
    }
    if (results.isEmpty() && StringUtils.isNotBlank(legalMiddleName)
        && legalFirstNameHasNoErrors(validationPayload)
        && StringUtils.isNotBlank(validationPayload.getLegalFirstName())
        && validationPayload.getLegalFirstName().contains(legalMiddleName)) {
      results.add(createValidationEntity(WARNING, EMBEDDED_MID, LEGAL_MID));
    }
    log.debug("transaction ID :: {} , returning results size :: {}", validationPayload.getTransactionID(), results.size());
    return results;
  }

  private boolean legalLastNameHasNoErrors(PenRequestBatchStudentValidationPayload validationPayload) {
    var result = validationPayload.getIssueList().stream().filter(element -> element.getPenRequestBatchValidationFieldCode().equals(LEGAL_LAST.getCode())).collect(Collectors.toList());
    return result.size() <= 0;
  }

  private boolean legalFirstNameHasNoErrors(PenRequestBatchStudentValidationPayload validationPayload) {
    var result = validationPayload.getIssueList().stream().filter(element -> element.getPenRequestBatchValidationFieldCode().equals(LEGAL_FIRST.getCode())).collect(Collectors.toList());
    return result.size() <= 0;
  }
}
