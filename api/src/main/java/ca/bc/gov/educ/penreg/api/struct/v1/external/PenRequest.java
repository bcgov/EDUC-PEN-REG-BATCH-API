package ca.bc.gov.educ.penreg.api.struct.v1.external;

import lombok.Data;

/**
 * The type Request.
 */
@Data
public class PenRequest {
  String mincode;
  String createUser;
  String updateUser;
  String localStudentID;
  String legalSurname;
  String legalGivenName;
  String legalMiddleName;
  String usualSurname;
  String usualGivenName;
  String usualMiddleName;
  String birthDate;
  String gender;
  String enrolledGradeCode;
  String postalCode;
}
