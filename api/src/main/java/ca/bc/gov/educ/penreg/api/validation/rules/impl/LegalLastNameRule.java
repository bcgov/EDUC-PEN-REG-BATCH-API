package ca.bc.gov.educ.penreg.api.validation.rules.impl;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.validation.rules.BaseRule;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationFieldCode.LEGAL_LAST;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueSeverityCode.ERROR;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueTypeCode.BLANK_FIELD;

/**
 * The type Legal last name rule.
 */
@Slf4j
public class LegalLastNameRule extends BaseRule {
  /**
   * Validate the Last Name.
   *
   * @param validationPayload the validation payload
   * @return the list
   */
  @Override
  public List<PenRequestBatchStudentValidationIssue> validate(PenRequestBatchStudentValidationPayload validationPayload) {
    final List<PenRequestBatchStudentValidationIssue> results = validationPayload.getIssueList();
    var legalLastName = validationPayload.getLegalLastName();
    if (legalLastName == null || "".equals(legalLastName.trim())) {
      results.add(createValidationEntity(ERROR, BLANK_FIELD, LEGAL_LAST));
    } else {
      defaultValidationForNameFields(results, legalLastName, LEGAL_LAST);
    }
    //PreReq: Skip this check if any of these issues has been reported for the current field: V2, V3, V4, V5, V6, V7, V8
    // to achieve above we do an empty check here and proceed only if there were no validation error till now, for this field.
    if (results.isEmpty()) {
      checkFieldValueExactMatchWithInvalidText(results, legalLastName, LEGAL_LAST, validationPayload.getIsInteractive());
    }
    log.debug("transaction ID :: {} , returning results size :: {}", validationPayload.getTransactionID(), results.size());
    return results;
  }




}
