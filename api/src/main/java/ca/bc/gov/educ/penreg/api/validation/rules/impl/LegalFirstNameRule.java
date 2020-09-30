package ca.bc.gov.educ.penreg.api.validation.rules.impl;

import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchValidOneLetterGivenNameCodeRepository;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.validation.rules.BaseRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationFieldCode.LEGAL_FIRST;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueSeverityCode.WARNING;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueTypeCode.*;

/**
 * The type Legal last name rule.
 */
@Slf4j
public class LegalFirstNameRule extends BaseRule {

  /**
   * The Valid one letter given name code repository.
   */
  private final PenRequestBatchValidOneLetterGivenNameCodeRepository validOneLetterGivenNameCodeRepository;

  /**
   * Instantiates a new Legal first name rule.
   *
   * @param validOneLetterGivenNameCodeRepository the valid one letter given name code repository
   */
  public LegalFirstNameRule(PenRequestBatchValidOneLetterGivenNameCodeRepository validOneLetterGivenNameCodeRepository) {
    this.validOneLetterGivenNameCodeRepository = validOneLetterGivenNameCodeRepository;
  }

  /**
   * Validate the Last Name.
   *
   * @param validationPayload the validation payload
   * @return the list
   */
  @Override
  public List<PenRequestBatchStudentValidationIssue> validate(PenRequestBatchStudentValidationPayload validationPayload) {
    final List<PenRequestBatchStudentValidationIssue> results = new LinkedList<>();
    var legalFirstName = validationPayload.getLegalFirstName();
    if (StringUtils.isBlank(legalFirstName)) {
      results.add(createValidationEntity(WARNING, BLANK_FIELD, LEGAL_FIRST));
    } else if (legalFirstName.trim().equals("'")) {
      results.add(createValidationEntity(WARNING, APOSTROPHE, LEGAL_FIRST));
    } else {
      defaultValidationForNameFields(results, legalFirstName, LEGAL_FIRST);
    }
    legalFirstName = legalFirstName.trim();
    //PreReq: Skip this check if any of these issues has been reported for the current field: V2, V3, V4, V5, V6, V7, V8
    // to achieve above we do an empty check here and proceed only if there were no validation error till now, for this field. V9 check.
    if (results.isEmpty()) {
      checkFieldValueExactMatchWithInvalidText(results, legalFirstName, LEGAL_FIRST, validationPayload.getIsInteractive());
    }
    if (results.isEmpty() && legalFirstName.length() == 1) { // if we dont have any validation
      var oneLetterGivenNames = validOneLetterGivenNameCodeRepository.findAll();
      boolean isMatched = false;
      for (var oneLetterGivenName : oneLetterGivenNames) {
        if (LocalDateTime.now().isAfter(oneLetterGivenName.getEffectiveDate())
            && LocalDateTime.now().isBefore(oneLetterGivenName.getExpiryDate())
            && oneLetterGivenName.getCode().equals(legalFirstName)) {
          isMatched = true;
        }
      }
      if (!isMatched) {
        results.add(createValidationEntity(WARNING, ONE_CHAR_NAME, LEGAL_FIRST));
      }
    }
    log.debug("transaction ID :: {} , returning results size :: {}", validationPayload.getTransactionID(), results.size());
    return results;
  }


}
