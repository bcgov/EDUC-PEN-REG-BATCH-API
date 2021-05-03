package ca.bc.gov.educ.penreg.api.struct.v1.external;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Pen request result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestResult {
  /**
   * The Validation issues.
   */
  List<PenRequestBatchStudentValidationIssue> validationIssues;
  /**
   * The Mincode.
   */
  Integer mincode;
  /**
   * The Local student id.
   */
  String localStudentID;
  /**
   * The Pen.
   */
  String pen;
  /**
   * The Legal surname.
   */
  String legalSurname;
  /**
   * The Legal given name.
   */
  String legalGivenName;
  /**
   * The Legal middle name.
   */
  String legalMiddleName;
  /**
   * The Usual surname.
   */
  String usualSurname;
  /**
   * The Usual given name.
   */
  String usualGivenName;
  /**
   * The Usual middle name.
   */
  String usualMiddleName;
  /**
   * The Birth date.
   */
  String birthDate;
  /**
   * The Gender.
   */
  String gender;
  /**
   * The Enrolled grade code.
   */
  String enrolledGradeCode;
  /**
   * The Postal code.
   */
  String postalCode;
}
