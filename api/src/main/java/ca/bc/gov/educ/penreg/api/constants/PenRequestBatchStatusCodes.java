package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch status codes.
 */
public enum PenRequestBatchStatusCodes {
  /**
   * Load fail pen request batch status codes.
   */
  LOAD_FAIL("LOADFAIL"),
  /**
   * Loaded pen request batch status codes.
   */
  LOADED("LOADED"),
  /**
   * Validated pen request batch status codes.
   */
  VALIDATED("VALIDATED"),
  /**
   * Active pen request batch status codes.
   */
  ACTIVE("ACTIVE"),
  /**
   * Unarchived pen request batch status codes.
   */
  UNARCHIVED("UNARCHIVED"),
  /**
   * Archived pen request batch status codes.
   */
  ARCHIVED("ARCHIVED");

  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch status codes.
   *
   * @param code the code
   */
  PenRequestBatchStatusCodes(String code) {
    this.code = code;
  }
}
