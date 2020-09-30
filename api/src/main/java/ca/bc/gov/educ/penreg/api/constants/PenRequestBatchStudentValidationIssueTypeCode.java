package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

public enum PenRequestBatchStudentValidationIssueTypeCode {
  ONE_CHAR_NAME("1CHARNAME"),
  APOSTROPHE("APOSTROPHE"),
  BLANK_FIELD("BLANKFIELD"),
  BLANK_IN_NAME("BLANKINNAME"),
  CHECK_DIGIT("CHKDIG"),
  DOB_INVALID("DOB_INVALID"),
  DOB_PAST("DOB_PAST"),
  DOB_FUTURE("DOB_FUTURE"),
  EMBEDDED_MID("EMBEDDEDMID"),
  GENDER_ERR("GENDER_ERR"),
  GRADE_CD_ERR("GRADECD_ERR"),
  INV_CHARS("INVCHARS"),
  INV_PREFIX("INVPREFIX"),
  OLD4GRADE("OLD4GRADE"),
  ON_BLOCK_LIST("ONBLOCKLIST"),
  REPEAT_MID("REPEATMID"),
  SCHAR_PREFIX("SCHARPREFIX"),
  YOUNG4GRADE("YOUNG4GRADE"),
  BEGIN_INVALID("BEGININVALID");

  @Getter
  private final String code;

  PenRequestBatchStudentValidationIssueTypeCode(String code) {
    this.code = code;
  }
}
