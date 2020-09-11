package ca.bc.gov.educ.penreg.api.constants;

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

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new Pen request batch student status codes.
   *
   * @param code the code
   */
  PenRequestBatchStudentStatusCodes(String code) {
    this.code = code;
  }
}
