package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

public enum PenRequestBatchStudentValidationFieldCode {
  LOCAL_ID("LOCALID"),
  SUBMITTED_PEN("SUBMITPEN"),
  LEGAL_FIRST("LEGALFIRST"),
  LEGAL_MID("LEGALMID"),
  LEGAL_LAST("LEGALLAST"),
  USUAL_FIRST("USUALFIRST"),
  USUAL_MID("USUALMID"),
  USUAL_LAST("USUALLAST"),
  POSTAL_CODE("POSTALCODE"),
  GRADE_CODE("GRADECODE"),
  BIRTH_DATE("BIRTHDATE"),
  GENDER("GENDER");

  @Getter
  private final String code;

  PenRequestBatchStudentValidationFieldCode(String code) {
    this.code = code;
  }
}
