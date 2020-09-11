package ca.bc.gov.educ.penreg.api.exception.errors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * The type Api validation error.
 */
@AllArgsConstructor
@Data
@Builder
@SuppressWarnings("squid:S1948")
public class ApiValidationError implements ApiSubError {
  /**
   * The Object.
   */
  private String object;
  /**
   * The Field.
   */
  private String field;
  /**
   * The Rejected value.
   */
  private Object rejectedValue;
  /**
   * The Message.
   */
  private String message;

  /**
   * Instantiates a new Api validation error.
   *
   * @param object  the object
   * @param message the message
   */
  ApiValidationError(String object, String message) {
    this.object = object;
    this.message = message;
  }
}
