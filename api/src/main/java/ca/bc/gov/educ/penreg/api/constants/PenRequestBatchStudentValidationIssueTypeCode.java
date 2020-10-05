package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student validation issue type code.
 */
public enum PenRequestBatchStudentValidationIssueTypeCode {
  /**
   * One char name pen request batch student validation issue type code.
   */
  ONE_CHAR_NAME("1CHARNAME"),
  /**
   * Apostrophe pen request batch student validation issue type code.
   */
  APOSTROPHE("APOSTROPHE"),
  /**
   * Blank field pen request batch student validation issue type code.
   */
  BLANK_FIELD("BLANKFIELD"),
  /**
   * Blank in name pen request batch student validation issue type code.
   */
  BLANK_IN_NAME("BLANKINNAME"),
  /**
   * Check digit pen request batch student validation issue type code.
   */
  CHECK_DIGIT("CHKDIG"),
  /**
   * Dob invalid pen request batch student validation issue type code.
   */
  DOB_INVALID("DOB_INVALID"),
  /**
   * Dob past pen request batch student validation issue type code.
   */
  DOB_PAST("DOB_PAST"),
  /**
   * Dob future pen request batch student validation issue type code.
   */
  DOB_FUTURE("DOB_FUTURE"),
  /**
   * Embedded mid pen request batch student validation issue type code.
   */
  EMBEDDED_MID("EMBEDDEDMID"),
  /**
   * Gender err pen request batch student validation issue type code.
   */
  GENDER_ERR("GENDER_ERR"),
  /**
   * Grade cd err pen request batch student validation issue type code.
   */
  GRADE_CD_ERR("GRADECD_ERR"),
  /**
   * Inv chars pen request batch student validation issue type code.
   */
  INV_CHARS("INVCHARS"),
  /**
   * Inv prefix pen request batch student validation issue type code.
   */
  INV_PREFIX("INVPREFIX"),
  /**
   * Old 4 grade pen request batch student validation issue type code.
   */
  OLD4GRADE("OLD4GRADE"),
  /**
   * On block list pen request batch student validation issue type code.
   */
  ON_BLOCK_LIST("ONBLOCKLIST"),
  /**
   * Repeat mid pen request batch student validation issue type code.
   */
  REPEAT_MID("REPEATMID"),
  /**
   * Schar prefix pen request batch student validation issue type code.
   */
  SCHAR_PREFIX("SCHARPREFIX"),
  /**
   * Young 4 grade pen request batch student validation issue type code.
   */
  YOUNG4GRADE("YOUNG4GRADE"),
  /**
   * Begin invalid pen request batch student validation issue type code.
   */
  BEGIN_INVALID("BEGININVALID");

  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch student validation issue type code.
   *
   * @param code the code
   */
  PenRequestBatchStudentValidationIssueTypeCode(String code) {
    this.code = code;
  }
}
