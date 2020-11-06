package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Twin Reason codes.
 */
@Getter
public enum TwinReasonCodes {
  /**
   * Twinned by Creating PEN.
   */
  PENCREATE("PENCREATE"),
  /**
   * Twinned by Creating PEN.
   */
  PEN_MATCH("PENMATCH");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new twin reason code.
   *
   * @param code the code
   */
  TwinReasonCodes(String code) {
    this.code = code;
  }
}
