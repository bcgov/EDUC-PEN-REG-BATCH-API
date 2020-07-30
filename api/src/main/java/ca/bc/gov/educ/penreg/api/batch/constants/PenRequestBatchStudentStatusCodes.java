package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student status codes.
 */
@Getter
public enum PenRequestBatchStudentStatusCodes {
  /**
   * Loaded pen request batch student status codes.
   */
  LOADED("LOADED"),
  /**
   * Error pen request batch student status codes.
   */
  ERROR("ERROR"),
  /**
   * Sys matched pen request batch student status codes.
   */
  SYS_MATCHED("SYSMATCHED"),
  /**
   * Sys new pen pen request batch student status codes.
   */
  SYS_NEW_PEN("SYSNEWPEN"),
  /**
   * Repeat pen request batch student status codes.
   */
  REPEAT("REPEAT"),
  /**
   * New fixable pen request batch student status codes.
   */
  NEW_FIXABLE("NEWFIXABLE"),
  /**
   * Usr matched pen request batch student status codes.
   */
  USR_MATCHED("USRMATCHED"),
  /**
   * Usr new pen pen request batch student status codes.
   */
  USR_NEW_PEN("USRNEWPEN"),
  /**
   * Returned pen request batch student status codes.
   */
  RETURNED("RETURNED");

  private final String code;

  PenRequestBatchStudentStatusCodes(String code) {
    this.code = code;
  }
}
