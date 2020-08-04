package ca.bc.gov.educ.penreg.api.batch.constants;

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

  private final String code;

  UnarchivedBatchStatusCodes(String code) {
    this.code = code;
  }
}


