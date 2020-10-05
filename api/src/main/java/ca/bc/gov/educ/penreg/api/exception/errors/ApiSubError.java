package ca.bc.gov.educ.penreg.api.exception.errors;

import java.io.Serializable;

/**
 * The interface Api sub error.
 */
public interface ApiSubError extends Serializable {

  /**
   * Gets field.
   *
   * @return the field
   */
  String getField();

  /**
   * Gets message.
   *
   * @return the message
   */
  String getMessage();

  /**
   * Gets object.
   *
   * @return the object
   */
  String getObject();

  /**
   * Gets rejected value.
   *
   * @return the rejected value
   */
  Object getRejectedValue();
}