package ca.bc.gov.educ.penreg.api.constants;

/**
 * The enum Saga topics enum.
 */
public enum SagaTopicsEnum {
  /**
   * Pen request batch student processing topic saga topics enum.
   */
  PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC,
  /**
   * Pen request batch issue processing topic saga topics enum.
   */
  PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC,

  /**
   * Pen request batch user match processing topic saga topics enum.
   */
  PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_TOPIC,
  /**
   * Pen match api topic saga topics enum.
   */
  PEN_MATCH_API_TOPIC,
  /**
   * Student api topic saga topics enum.
   */
  STUDENT_API_TOPIC,
  /**
   * Pen validation api topic saga topics enum.
   */
  PEN_SERVICES_API_TOPIC,
  /**
   * Pen request batch api topic saga topics enum.
   */
  PEN_REQUEST_BATCH_API_TOPIC
}
