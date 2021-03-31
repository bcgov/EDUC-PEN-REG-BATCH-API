package ca.bc.gov.educ.penreg.api.constants;

/**
 * The enum Event type.
 */
public enum EventType {
  /**
   * Read from topic event type.
   */
  READ_FROM_TOPIC,
  /**
   * Process pen match event type.
   */
  PROCESS_PEN_MATCH,
  /**
   * Initiated event type.
   */
  INITIATED,
  /**
   * Mark saga complete event type.
   */
  MARK_SAGA_COMPLETE,
  /**
   * Process pen match results event type.
   */
  PROCESS_PEN_MATCH_RESULTS,
  /**
   * Get student event type.
   */
  GET_STUDENT,
  /**
   * Create student event type.
   */
  CREATE_STUDENT,
  /**
   * Update student event type.
   */
  UPDATE_STUDENT,
  /**
   * Validate student demographics event type.
   */
  VALIDATE_STUDENT_DEMOGRAPHICS,
  /**
   * Get the next PEN number event type.
   */
  GET_NEXT_PEN_NUMBER,
  /**
   * Update pen request batch student event type.
   */
  UPDATE_PEN_REQUEST_BATCH_STUDENT,
  /**
   * Pen request batch event outbox processed event type.
   */
  PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED,
  /**
   * Add student twins event type.
   */
  ADD_POSSIBLE_MATCH,
  /**
   * Delete possible match event type.
   */
  DELETE_POSSIBLE_MATCH,
  /**
   * Pen request batch notify school file format error event type.
   */
  PEN_REQUEST_BATCH_NOTIFY_SCHOOL_FILE_FORMAT_ERROR,
  /**
   * Pen request batch event update pen request batch event type.
   */
  UPDATE_PEN_REQUEST_BATCH,
  /**
   * Get pen coordinator event type.
   */
  GET_PEN_COORDINATOR,
  /**
   * Notify pen request batch archive has contact event type.
   */
  NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT,
  /**
   * Notify pen request batch archive has no school contact event type.
   */
  NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT,
  /**
   * Get matched students event type.
   */
  GET_STUDENTS,
  /**
   * generate pen request batch reports event type
   */
  GENERATE_PEN_REQUEST_BATCH_REPORTS,
  /**
   * Gather report data event type
   */
  GATHER_REPORT_DATA,
  /**
   * Save pdf report event type
   */
  SAVE_REPORTS
}
