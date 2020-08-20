package ca.bc.gov.educ.penreg.api.batch.constants;

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
  @Getter
  private final String code;

  PenRequestBatchEventCodes(String code) {
    this.code = code;
  }
}
