package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.validation.constraints.*;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements Serializable {
  private static final long serialVersionUID = 1L;

  String studentID;
  @NotNull(message = "PEN Number can not be null.")
  String pen;
  @Size(max = 40)
  @NotNull(message = "Legal First Name can not be null.")
  String legalFirstName;
  @Size(max = 60)
  String legalMiddleNames;
  @Size(max = 40)
  @NotNull(message = "Legal Last Name can not be null.")
  String legalLastName;
  @NotNull(message = "Date of Birth can not be null.")
  String dob;
  @NotNull(message = "Sex Code can not be null.")
  String sexCode;
  String genderCode;
  @Size(max = 40)
  String usualFirstName;
  @Size(max = 60)
  String usualMiddleNames;
  @Size(max = 40)
  String usualLastName;
  @Size(max = 80)
  @Email(message = "Email must be valid email address.")
  String email;
  @NotNull(message = "Email verified cannot be null.")
  @Size(max = 1)
  @Pattern(regexp = "[YN]")
  String emailVerified;
  String deceasedDate;
  @Column(name = "postal_code")
  @Size(max = 7)
  @Pattern(regexp = "^([A-Z]\\d[A-Z]\\d[A-Z]\\d|)$")
  String postalCode;
  @Size(max = 8)
  String mincode;
  @Size(max = 12)
  String localID;
  @Size(max = 2)
  String gradeCode;
  @Size(max = 4)
  String gradeYear;
  @Size(max = 1)
  String demogCode;
  @Size(max = 1)
  String statusCode;
  @Size(max = 25)
  String memo;
  @Size(max = 32)
  String createUser;
  @Size(max = 32)
  String updateUser;
  @Null(message = "createDate should be null.")
  String createDate;
  @Null(message = "updateDate should be null.")
  String updateDate;
}
