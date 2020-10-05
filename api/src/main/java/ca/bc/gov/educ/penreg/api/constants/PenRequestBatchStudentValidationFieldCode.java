package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student validation field code.
 */
public enum PenRequestBatchStudentValidationFieldCode {
  /**
   * Local id pen request batch student validation field code.
   */
  LOCAL_ID("LOCALID"),
  /**
   * Submitted pen pen request batch student validation field code.
   */
  SUBMITTED_PEN("SUBMITPEN"),
  /**
   * Legal first pen request batch student validation field code.
   */
  LEGAL_FIRST("LEGALFIRST"),
  /**
   * Legal mid pen request batch student validation field code.
   */
  LEGAL_MID("LEGALMID"),
  /**
   * Legal last pen request batch student validation field code.
   */
  LEGAL_LAST("LEGALLAST"),
  /**
   * Usual first pen request batch student validation field code.
   */
  USUAL_FIRST("USUALFIRST"),
  /**
   * Usual mid pen request batch student validation field code.
   */
  USUAL_MID("USUALMID"),
  /**
   * Usual last pen request batch student validation field code.
   */
  USUAL_LAST("USUALLAST"),
  /**
   * Postal code pen request batch student validation field code.
   */
  POSTAL_CODE("POSTALCODE"),
  /**
   * Grade code pen request batch student validation field code.
   */
  GRADE_CODE("GRADECODE"),
  /**
   * Birth date pen request batch student validation field code.
   */
  BIRTH_DATE("BIRTHDATE"),
  /**
   * Gender pen request batch student validation field code.
   */
  GENDER("GENDER");

  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch student validation field code.
   *
   * @param code the code
   */
  PenRequestBatchStudentValidationFieldCode(String code) {
    this.code = code;
  }
}
