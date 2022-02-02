package ca.bc.gov.educ.penreg.api.batch.mappers;

import org.apache.commons.lang3.StringUtils;

/**
 * The type String mapper.
 */
public final class StringMapper {

  private StringMapper() {

  }

  /**
   * Map string.
   *
   * @param value the value
   * @return the string
   */
  public static String map(final String value) {
    if (StringUtils.isNotBlank(value)) {
      return value.trim();
    }
    return value;
  }

  public static String uppercaseAndCleanDiacriticalMarks(String value){
    if (StringUtils.isNotBlank(value)) {
      return StringUtils.stripAccents(value).toUpperCase();
    }
    return value;
  }
}
