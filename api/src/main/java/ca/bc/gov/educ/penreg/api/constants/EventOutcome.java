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
   * Saga completed event outcome.
   */
  SAGA_COMPLETED,
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
   * Student already exist event outcome.
   */
  STUDENT_ALREADY_EXIST,
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
  VALIDATION_SUCCESS_WITH_ONLY_WARNING,
  /**
   * Next PEN number retrieved event outcome.
   */
  NEXT_PEN_NUMBER_RETRIEVED,
  /**
   * PEN request batch student updated event outcome.
   */
  PEN_REQUEST_BATCH_STUDENT_UPDATED,
  /**
   * PEN request batch student not found event outcome.
   */
  PEN_REQUEST_BATCH_STUDENT_NOT_FOUND,
  /**
   * Possible match added event outcome.
   */
  POSSIBLE_MATCH_ADDED,
  /**
   * Possible match deleted event outcome.
   */
  POSSIBLE_MATCH_DELETED,
  /**
   * Pen request batch updated event outcome.
   */
  PEN_REQUEST_BATCH_UPDATED,
  /**
   * Pen request batch not found event outcome.
   */
  PEN_REQUEST_BATCH_NOT_FOUND,
  /**
   * Pen coordinator found outcome
   */
  PEN_COORDINATOR_FOUND,
  /**
   * Pen coordinator not found outcome
   */
  PEN_COORDINATOR_NOT_FOUND,
  /**
   * Archive email sent event outcome
   */
  ARCHIVE_EMAIL_SENT,
  /**
   * Archive pen request batch reports generated event outcome
   */
  ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED,
  /**
   * Students found event outcome
   */
  STUDENTS_FOUND,
  /**
   * Students not found event outcome
   */
  STUDENTS_NOT_FOUND,
  /**
   * Report data gathered event outcome
   */
  REPORT_DATA_GATHERED,
  /**
   * Pdf report saved event outcome
   */
  REPORTS_SAVED
}
