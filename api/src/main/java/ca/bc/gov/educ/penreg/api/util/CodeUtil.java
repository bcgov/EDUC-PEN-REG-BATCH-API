package ca.bc.gov.educ.penreg.api.util;

import ca.bc.gov.educ.penreg.api.constants.GenderCodes;
import ca.bc.gov.educ.penreg.api.constants.SexCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Optional;

import static java.time.temporal.ChronoField.*;

/**
 * The code util.
 */
public final class CodeUtil {

  /**
   * Instantiates a new code util.
   */
  private CodeUtil() {
  }

  /**
   * Get the sex code according to the gender code
   *
   * @param genderCode the gender code
   * @return the sex code
   */
  public static String getSexCodeFromGenderCode(String genderCode) {
    if (GenderCodes.X.getCode().equals(genderCode)) {
      return SexCodes.U.getCode();
    }
    return genderCode;
  }
}
