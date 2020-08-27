package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum School group codes.
 */
@Getter
public enum SchoolGroupCodes {
  /**
   * K 12 school group codes.
   */
  K12("K12"),
  /**
   * Psi school group codes.
   */
  PSI("PSI");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new School group codes.
   *
   * @param code the code
   */
  SchoolGroupCodes(String code) {
    this.code = code;
  }
}
