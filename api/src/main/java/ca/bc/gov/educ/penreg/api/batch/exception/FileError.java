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
   * Invalid trailer
   */
  INVALID_TRAILER("Invalid trailer record. Student count could not be retrieved"),
  /**
   * Invalid trailer for student count
   */
  INVALID_TRAILER_STUDENT_COUNT("Invalid trailer record. Student count was not a numeric value"),
  /**
   * The Invalid transaction code student details.
   */
  INVALID_TRANSACTION_CODE_STUDENT_DETAILS("Invalid transaction code on Detail record $? for student with Local ID $?"),

  /**
   * Invalid row length file error.
   * This will be thrown when any row in the given file is longer or shorter than expected.
   */
  INVALID_ROW_LENGTH("$?"),
  /**
   * The mincode school is currently closed
   */
  INVALID_MINCODE_SCHOOL_CLOSED("Invalid Mincode in Header record - school is closed."),

  /**
   * The Duplicate batch file psi.
   */
  DUPLICATE_BATCH_FILE_PSI("Duplicate file from PSI."),

  /**
   * The Held back for size.
   */
  HELD_BACK_FOR_SIZE("Held Back For Size."),

  /**
   * The held back for sfas code
   */
  HELD_BACK_FOR_SFAS("Held back for SFAS.");

  /**
   * The Message.
   */
  @Getter
  private final String message;

  /**
   * Instantiates a new File error.
   *
   * @param message the message
   */
  FileError(final String message) {
    this.message = message;
  }
}
