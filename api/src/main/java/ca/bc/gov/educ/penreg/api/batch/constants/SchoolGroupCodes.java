package ca.bc.gov.educ.penreg.api.batch.constants;

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

  private final String code;

  SchoolGroupCodes(String code) {
    this.code = code;
  }
}
