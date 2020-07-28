package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

@Getter
public enum MinistryPRBSourceCodes {
  TSW_PEN_WEB("TSWPENWEB");

  private final String code;

  MinistryPRBSourceCodes(String code) {
    this.code = code;
  }
}
