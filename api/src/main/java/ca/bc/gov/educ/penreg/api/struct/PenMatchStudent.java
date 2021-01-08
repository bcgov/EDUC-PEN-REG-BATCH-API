package ca.bc.gov.educ.penreg.api.struct;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Pen match student.
 */
@Data
@NoArgsConstructor
public class PenMatchStudent {
  /**
   * The Pen.
   */
  protected String pen;
  /**
   * The Dob.
   */
  protected String dob;
  /**
   * The Sex.
   */
  protected String sex;
  /**
   * The Enrolled grade code.
   */
  protected String enrolledGradeCode;
  /**
   * The Surname.
   */
  protected String surname;
  /**
   * The Given name.
   */
  protected String givenName;
  /**
   * The Middle name.
   */
  protected String middleName;
  /**
   * The Usual surname.
   */
  protected String usualSurname;
  /**
   * The Usual given name.
   */
  protected String usualGivenName;
  /**
   * The Usual middle name.
   */
  protected String usualMiddleName;
  /**
   * The mincode.
   */
  protected String mincode;
  /**
   * The Local id.
   */
  protected String localID;
  /**
   * The Postal.
   */
  protected String postal;
}
