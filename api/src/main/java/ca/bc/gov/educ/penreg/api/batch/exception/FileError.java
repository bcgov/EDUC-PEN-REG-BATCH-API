package ca.bc.gov.educ.penreg.api.batch.exception;

import lombok.Getter;

/**
 * The enum File error.
 *
 * @author OM
 */
public enum FileError {
  /**
   * The Invalid transaction code header.
   */
  INVALID_TRANSACTION_CODE_HEADER("Invalid transaction code on Header record. It must be FFI"),
  /**
   * The Invalid transaction code trailer.
   */
  INVALID_TRANSACTION_CODE_TRAILER("Invalid transaction code on Trailer record. It must be BTR"),
  /**
   * The Invalid mincode header.
   */
  INVALID_MINCODE_HEADER("Invalid Mincode in Header record."),
  /**
   * The Student count mismatch.
   */
  STUDENT_COUNT_MISMATCH("Invalid count in trailer record. Stated was $?, Actual was $?"),
  /**
   * The Invalid transaction code student details.
   */
  INVALID_TRANSACTION_CODE_STUDENT_DETAILS("Invalid transaction code on Detail record $? for student with Local ID $?"),

  /**
   * Invalid row length file error.
   * This will be thrown when any row in the given file is longer or shorter than expected.
   */
  INVALID_ROW_LENGTH("$?");

  @Getter
  private final String message;

  FileError(String message) {
    this.message = message;
  }
}
