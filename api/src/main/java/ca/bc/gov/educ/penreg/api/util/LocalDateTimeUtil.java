package ca.bc.gov.educ.penreg.api.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Optional;

import static java.time.temporal.ChronoField.*;

public class LocalDateTimeUtil {

  /**
   * Get api formatted date of birth string.
   *
   * @param dateOfBirth the date of birth
   * @return the string
   */
  public static String getAPIFormattedDateOfBirth(String dateOfBirth){
    if(Optional.ofNullable(dateOfBirth).isEmpty()){
      return null;
    }
    DateTimeFormatter ISO_LOCAL_DATE = new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .appendValue(MONTH_OF_YEAR, 2)
      .appendValue(DAY_OF_MONTH, 2).toFormatter();
    LocalDate date = LocalDate.parse( dateOfBirth, ISO_LOCAL_DATE );
    return date.format(DateTimeFormatter.ISO_DATE);
  }
}
