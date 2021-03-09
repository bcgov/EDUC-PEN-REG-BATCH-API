package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Twin Reason codes.
 */
@Getter
public enum PenRequestBatchProcessTypeCodes {
  /**
   * Used API to process.
   */
  API("API"),
  /**
   * Used flat file to process.
   */
  FLAT_FILE("FLAT_FILE");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new twin reason code.
   *
   * @param code the code
   */
  PenRequestBatchProcessTypeCodes(String code) {
    this.code = code;
  }
}
