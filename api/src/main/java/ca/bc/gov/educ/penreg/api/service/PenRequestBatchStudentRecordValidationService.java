package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.validation.rules.Rule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The type Pen request batch student record validation service.
 */
@Service
@Slf4j
public class PenRequestBatchStudentRecordValidationService {

  /**
   * The Rules.
   */
  private final List<Rule> rules;

  /**
   * Instantiates a new Pen request batch student record validation service.
   *
   * @param rules the rules
   */
  @Autowired
  public PenRequestBatchStudentRecordValidationService(List<Rule> rules) {
    this.rules = rules;
  }

  /**
   * This method implements the data validation as mentioned here
   * <pre>
   *  <a href="https://gww.wiki.educ.gov.bc.ca/display/PEN/Story%3A+v1+Pre-Match+Validation+of+PEN+Request+Data"></a>
   * </pre>
   * Validate student record and return the result as a list.
   *
   * @param validationPayload the validation payload
   * @return the list
   */
  public List<PenRequestBatchStudentValidationIssue> validateStudentRecord(final PenRequestBatchStudentValidationPayload validationPayload) {
    var validationResult = validationPayload.getIssueList();
    rules.forEach(rule -> {
      var result = rule.validate(validationPayload);
      if (!result.isEmpty()) {
        validationResult.addAll(result);
      }
    });
    return validationResult;
  }

}
