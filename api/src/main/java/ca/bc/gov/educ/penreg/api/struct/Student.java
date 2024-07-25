package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * The type Student.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The Student id.
   */
  String studentID;
  /**
   * The Pen.
   */
  @NotNull(message = "PEN Number can not be null.")
  String pen;
  /**
   * The Legal first name.
   */
  @Size(max = 40)
  @NotNull(message = "Legal First Name can not be null.")
  String legalFirstName;
  /**
   * The Legal middle names.
   */
  @Size(max = 60)
  String legalMiddleNames;
  /**
   * The Legal last name.
   */
  @Size(max = 40)
  @NotNull(message = "Legal Last Name can not be null.")
  String legalLastName;
  /**
   * The Dob.
   */
  @NotNull(message = "Date of Birth can not be null.")
  String dob;
  /**
   * The Sex code.
   */
  @NotNull(message = "Sex Code can not be null.")
  String sexCode;
  /**
   * The Gender code.
   */
  String genderCode;
  /**
   * The Usual first name.
   */
  @Size(max = 40)
  String usualFirstName;
  /**
   * The Usual middle names.
   */
  @Size(max = 60)
  String usualMiddleNames;
  /**
   * The Usual last name.
   */
  @Size(max = 40)
  String usualLastName;
  /**
   * The Email.
   */
  @Size(max = 80)
  @Email(message = "Email must be valid email address.")
  String email;
  /**
   * The Email verified.
   */
  @NotNull(message = "Email verified cannot be null.")
  @Size(max = 1)
  @Pattern(regexp = "[YN]")
  String emailVerified;
  /**
   * The Deceased date.
   */
  String deceasedDate;
  /**
   * The Postal code.
   */
  @Size(max = 7)
  @Pattern(regexp = "^([A-Z]\\d[A-Z]\\d[A-Z]\\d|)$")
  String postalCode;
  /**
   * The mincode.
   */
  @Size(max = 8)
  String mincode;
  /**
   * The Local id.
   */
  @Size(max = 12)
  String localID;
  /**
   * The Grade code.
   */
  @Size(max = 2)
  String gradeCode;
  /**
   * The Grade year.
   */
  @Size(max = 4)
  String gradeYear;
  /**
   * The Demog code.
   */
  @Size(max = 1)
  String demogCode;
  /**
   * The Status code.
   */
  @Size(max = 1)
  String statusCode;
  /**
   * The Memo.
   */
  @Size(max = 25)
  String memo;
  /**
   * The Create user.
   */
  @Size(max = 100)
  String createUser;
  /**
   * The Update user.
   */
  @Size(max = 100)
  String updateUser;

  String historyActivityCode;

  String trueStudentID;
  String documentTypeCode;

  String dateOfConfirmation;
}
