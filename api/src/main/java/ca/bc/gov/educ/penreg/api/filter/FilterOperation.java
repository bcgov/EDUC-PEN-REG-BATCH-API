package ca.bc.gov.educ.penreg.api.filter;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum FilterOperation {

  EQUAL("eq"),
  NOT_EQUAL("neq"),
  GREATER_THAN("gt"),
  GREATER_THAN_OR_EQUAL_TO("gte"),
  LESS_THAN("lt"),
  LESS_THAN_OR_EQUAL_TO("lte"),
  IN("in"),
  NOT_IN("nin"),
  BETWEEN("btn"),
  CONTAINS("like"),
  CONTAINS_IGNORE_CASE("like_ignore_case"),
  STARTS_WITH("starts_with"),
  STARTS_WITH_IGNORE_CASE("starts_with_ignore_case");

  private final String value;

  FilterOperation(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  public static Optional<FilterOperation> fromValue(String value) {
    for (FilterOperation op : FilterOperation.values()) {
      if (String.valueOf(op.value).equalsIgnoreCase(value)) {
        return Optional.of(op);
      }
    }
    return Optional.empty();
  }

}
