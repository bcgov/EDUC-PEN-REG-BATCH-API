package ca.bc.gov.educ.penreg.api.mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.temporal.ChronoField.*;

/**
 * The type Local date time mapper.
 */
public class LocalDateTimeMapper {

  /**
   * Map string.
   *
   * @param dateTime the date time
   * @return the string
   */
  public String map(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
  }

  /**
   * Map local date time.
   *
   * @param dateTime the date time
   * @return the local date time
   */
  public LocalDateTime map(String dateTime) {
    if (dateTime == null) {
      return null;
    }
    return LocalDateTime.parse(dateTime);
  }

}
