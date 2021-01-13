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
}
