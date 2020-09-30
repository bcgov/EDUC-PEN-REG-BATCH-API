package ca.bc.gov.educ.penreg.api.exception.errors;

import java.io.Serializable;

/**
 * The interface Api sub error.
 */
public interface ApiSubError extends Serializable {

  String getField();

  String getMessage();

  String getObject();

  Object getRejectedValue();
}