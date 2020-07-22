package ca.bc.gov.educ.penreg.api.exception.errors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
@SuppressWarnings("squid:S1948")
public class ApiValidationError implements ApiSubError {
  private String object;
  private String field;
  private Object rejectedValue;
  private String message;

  ApiValidationError(String object, String message) {
    this.object = object;
    this.message = message;
  }
}
