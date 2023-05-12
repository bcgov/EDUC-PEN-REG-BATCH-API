package ca.bc.gov.educ.penreg.api.util;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Optional;

/**
 * The type Local date time util.
 */
public final class LocalDateTimeUtil {

  /**
   * Instantiates a new Local date time util.
   */
  private LocalDateTimeUtil() {
  }

  /**
   * Get api formatted date of birth string.
   *
   * @param dateOfBirth the date of birth
   * @return the string
   */
  public static String getAPIFormattedDateOfBirth(String dateOfBirth) {
    if (Optional.ofNullable(dateOfBirth).isEmpty()) {
      return null;
    }
    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
        .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendValue(MONTH_OF_YEAR, 2)
        .appendValue(DAY_OF_MONTH, 2).toFormatter();
    LocalDate date = LocalDate.parse(dateOfBirth, dateTimeFormatter);
    return date.format(DateTimeFormatter.ISO_DATE);
  }
}
