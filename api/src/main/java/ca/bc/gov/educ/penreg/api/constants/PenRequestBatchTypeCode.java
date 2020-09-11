package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch type code.
 */
@Getter
public enum PenRequestBatchTypeCode {
  /**
   * School pen request batch type code.
   */
  SCHOOL("SCHOOL"),
  /**
   * Nom roll pen request batch type code.
   */
  NOM_ROLL("NOMROLL"),
  /**
   * Sld pen request batch type code.
   */
  SLD("SLD");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new Pen request batch type code.
   *
   * @param code the code
   */
  PenRequestBatchTypeCode(String code) {
    this.code = code;
  }
}
