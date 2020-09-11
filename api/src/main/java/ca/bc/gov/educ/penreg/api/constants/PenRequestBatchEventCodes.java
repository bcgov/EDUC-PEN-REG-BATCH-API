package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch event codes.
 */
public enum PenRequestBatchEventCodes {
  /**
   * Status changed pen request batch event codes.
   */
  STATUS_CHANGED("STATUSCHG"),
  /**
   * Returned pen request batch event codes.
   */
  RETURNED("RETURNED");
  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch event codes.
   *
   * @param code the code
   */
  PenRequestBatchEventCodes(String code) {
    this.code = code;
  }
}
