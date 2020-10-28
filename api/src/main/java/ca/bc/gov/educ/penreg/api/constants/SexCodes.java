package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Sex codes.
 */
@Getter
public enum SexCodes {
  /**
   * Unknown.
   */
  U("U");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new sex code.
   *
   * @param code the code
   */
  SexCodes(String code) {
    this.code = code;
  }
}
