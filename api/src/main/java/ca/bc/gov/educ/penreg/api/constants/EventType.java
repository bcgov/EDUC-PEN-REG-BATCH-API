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
  UPDATE_STUDENT
}
