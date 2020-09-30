package ca.bc.gov.educ.penreg.api.validation.rules.impl;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.validation.rules.BaseRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationFieldCode.USUAL_MID;

/**
 * The type Usual middle name rule.
 */
@Slf4j
public class UsualMiddleNameRule extends BaseRule {
  /**
   * Validates the student record for the given rule.
   *
   * @param validationPayload the validation payload
   * @return the validation result as a list.
   */
  @Override
  public List<PenRequestBatchStudentValidationIssue> validate(PenRequestBatchStudentValidationPayload validationPayload) {
    final List<PenRequestBatchStudentValidationIssue> results = new LinkedList<>();
    var usualMiddleNames = validationPayload.getUsualMiddleNames();
    if (StringUtils.isNotBlank(usualMiddleNames)) {
      defaultValidationForNameFields(results, usualMiddleNames, USUAL_MID);
    }
    if (results.isEmpty()) {
      checkFieldValueExactMatchWithInvalidText(results, usualMiddleNames, USUAL_MID, validationPayload.getIsInteractive());
    }
    log.debug("transaction ID :: {} , returning results size :: {}", validationPayload.getTransactionID(), results.size());
    return results;
  }
}
