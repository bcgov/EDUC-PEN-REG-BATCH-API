package ca.bc.gov.educ.penreg.api.constants;

import java.util.HashMap;
import java.util.Map;
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
  SYS_MATCHED("MATCHEDSYS"),
  /**
   * Sys new pen pen request batch student status codes.
   */
  SYS_NEW_PEN("NEWPENSYS"),
  /**
   * Repeat pen request batch student status codes.
   */
  REPEAT("REPEAT"),
  /**
   * New fixable pen request batch student status codes.
   */
  FIXABLE("FIXABLE"),
  /**
   * Usr matched pen request batch student status codes.
   */
  USR_MATCHED("MATCHEDUSR"),
  /**
   * Usr new pen pen request batch student status codes.
   */
  USR_NEW_PEN("NEWPENUSR"),
  /**
   * Info requested request batch student status codes.
   */
  INFOREQ("INFOREQ"),

  /**
   * Duplicate request batch student status codes.
   */
  DUPLICATE("DUPLICATE");

  /**
   * The constant codeMap.
   */
  private static final Map<String, PenRequestBatchStudentStatusCodes> codeMap = new HashMap<>();

  static {
    for (PenRequestBatchStudentStatusCodes status: values()) {
      codeMap.put(status.getCode(), status);
    }
  }

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

  /**
   * To string string.
   *
   * @return the string
   */
  @Override
  public String toString(){
    return this.getCode();
  }

  /**
   * Value of code pen request batch student status codes.
   *
   * @param code the code
   * @return the pen request batch student status codes
   */
  public static PenRequestBatchStudentStatusCodes valueOfCode(String code) {
    return codeMap.get(code);
  }
}
