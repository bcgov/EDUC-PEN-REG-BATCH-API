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
  GET_STUDENT,
  CREATE_STUDENT,
  UPDATE_STUDENT
}
