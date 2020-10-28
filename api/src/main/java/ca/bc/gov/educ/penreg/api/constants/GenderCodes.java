package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Gender codes.
 */
@Getter
public enum GenderCodes {
  /**
   * Gender Diverse.
   */
  X("X");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new gender code.
   *
   * @param code the code
   */
  GenderCodes(String code) {
    this.code = code;
  }
}
