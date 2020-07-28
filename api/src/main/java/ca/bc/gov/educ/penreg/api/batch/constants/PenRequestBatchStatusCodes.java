package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

/**
 * The enum Pen request batch status codes.
 */
public enum PenRequestBatchStatusCodes {
  /**
   * New pen request batch status codes.
   */
  NEW("NEW"),
  /**
   * Load fail pen request batch status codes.
   */
  LOAD_FAIL("LOADFAIL"),
  /**
   * Loaded pen request batch status codes.
   */
  LOADED("LOADED");

  @Getter
  private final String code;

  PenRequestBatchStatusCodes(String code) {
    this.code = code;
  }
}
