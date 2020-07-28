package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

/**
 * The enum Ministry prb source codes.
 */
@Getter
public enum MinistryPRBSourceCodes {
  /**
   * Tsw pen web ministry prb source codes.
   */
  TSW_PEN_WEB("TSWPENWEB");

  private final String code;

  MinistryPRBSourceCodes(String code) {
    this.code = code;
  }
}
