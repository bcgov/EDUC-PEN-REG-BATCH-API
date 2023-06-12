package ca.bc.gov.educ.penreg.api.filter;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;

/**
 * The enum Filter operation.
 */
public enum FilterOperation {

  /**
   * Equal filter operation.
   */
  EQUAL("eq"),
  /**
   * Not equal filter operation.
   */
  NOT_EQUAL("neq"),
  /**
   * Greater than filter operation.
   */
  GREATER_THAN("gt"),
  /**
   * Greater than or equal to filter operation.
   */
  GREATER_THAN_OR_EQUAL_TO("gte"),
  /**
   * Less than filter operation.
   */
  LESS_THAN("lt"),
  /**
   * Less than or equal to filter operation.
   */
  LESS_THAN_OR_EQUAL_TO("lte"),
  /**
   * In filter operation.
   */
  IN("in"),
  /**
   * Not in filter operation.
   */
  NOT_IN("nin"),
  /**
   * Between filter operation.
   */
  BETWEEN("btn"),
  /**
   * Contains filter operation.
   */
  CONTAINS("like"),
  /**
   * Contains ignore case filter operation.
   */
  CONTAINS_IGNORE_CASE("like_ignore_case"),
  /**
   * Starts with filter operation.
   */
  STARTS_WITH("starts_with"),
  /**
   * Starts with ignore case filter operation.
   */
  STARTS_WITH_IGNORE_CASE("starts_with_ignore_case");

  /**
   * The Value.
   */
  private final String value;

  /**
   * Instantiates a new Filter operation.
   *
   * @param value the value
   */
  FilterOperation(String value) {
    this.value = value;
  }

  /**
   * To string string.
   *
   * @return the string
   */
  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  /**
   * From value optional.
   *
   * @param value the value
   * @return the optional
   */
  public static Optional<FilterOperation> fromValue(String value) {
    for (FilterOperation op : FilterOperation.values()) {
      if (String.valueOf(op.value).equalsIgnoreCase(value)) {
        return Optional.of(op);
      }
    }
    return Optional.empty();
  }

}
