package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Unarchived batch status codes.
 */
@Getter
public enum UnarchivedBatchStatusCodes {
  /**
   * Na unarchived batch status codes.
   */
  NA("NA"),
  /**
   * Unchanged unarchived batch status codes.
   */
  UNCHANGED("UNCHANGED"),
  /**
   * Changed unarchived batch status codes.
   */
  CHANGED("CHANGED"),
  /**
   * Rearchived unarchived batch status codes.
   */
  REARCHIVED("REARCHIVED");

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new Unarchived batch status codes.
   *
   * @param code the code
   */
  UnarchivedBatchStatusCodes(String code) {
    this.code = code;
  }
}


