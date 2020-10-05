package ca.bc.gov.educ.penreg.api.constants;

/**
 * The enum Event outcome.
 */
public enum EventOutcome {
  /**
   * Read from topic success event outcome.
   */
  READ_FROM_TOPIC_SUCCESS,
  /**
   * Initiate success event outcome.
   */
  INITIATE_SUCCESS,
  /**
   * Pen match processed event outcome.
   */
  PEN_MATCH_PROCESSED,
  /**
   * Student found event outcome.
   */
  STUDENT_FOUND,
  /**
   * Student not found event outcome.
   */
  STUDENT_NOT_FOUND,
  /**
   * Student created event outcome.
   */
  STUDENT_CREATED,
  /**
   * Student updated event outcome.
   */
  STUDENT_UPDATED,
  /**
   * Pen match results processed event outcome.
   */
  PEN_MATCH_RESULTS_PROCESSED,
  /**
   * Validation success no error warning event outcome.
   */
  VALIDATION_SUCCESS_NO_ERROR_WARNING,
  /**
   * Validation success with error event outcome.
   */
  VALIDATION_SUCCESS_WITH_ERROR,
  /**
   * Validation success with only warning event outcome.
   */
  VALIDATION_SUCCESS_WITH_ONLY_WARNING
}
