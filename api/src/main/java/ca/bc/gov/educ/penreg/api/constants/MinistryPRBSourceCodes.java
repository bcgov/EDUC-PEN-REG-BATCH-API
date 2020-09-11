package ca.bc.gov.educ.penreg.api.constants;

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

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new Ministry prb source codes.
   *
   * @param code the code
   */
  MinistryPRBSourceCodes(String code) {
    this.code = code;
  }
}
