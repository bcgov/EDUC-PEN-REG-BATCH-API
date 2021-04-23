package ca.bc.gov.educ.penreg.api.struct.v1.external;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import lombok.Data;

import java.util.List;

@Data
public class ListItem {

  String mincode;
  String pen;
  String localID;
  String legalSurname;
  String legalGivenName;
  String legalMiddleNames;
  String usualSurname;
  String usualGivenName;
  String usualMiddleNames;
  String birthDate;
  String gender;
  String enrolledGradeCode;
  String postalCode;
  List<PenRequestBatchStudentValidationIssue> validationIssues;
}
