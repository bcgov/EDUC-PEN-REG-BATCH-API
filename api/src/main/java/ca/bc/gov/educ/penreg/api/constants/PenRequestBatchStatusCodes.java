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
   * Checked pen request batch for repeats.
   */
  REPEATS_CHECKED("RPTCHEKED"),
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
   * Changed pen request batch status codes while unarchived.
   */
  UNARCHIVED_CHANGED("UNARCH_CHG"),
  /**
   * Archived pen request batch status codes.
   */
  ARCHIVED("ARCHIVED"),
  /**
   * Re-Archived pen request batch status codes.
   */
  REARCHIVED("REARCHIVED"),
  /**
   * Status code for loads held back for size.
   */
  HOLD_FOR_REVIEW("HOLD_SIZE"),
  /**
   * Deleted pen request batch status codes.
   */
  DELETED("DELETED"),
  /**
   * Duplicate pen request batch status codes.
   */
  DUPLICATE("DUPLICATE");
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
  PenRequestBatchStatusCodes(final String code) {
    this.code = code;
  }
}
