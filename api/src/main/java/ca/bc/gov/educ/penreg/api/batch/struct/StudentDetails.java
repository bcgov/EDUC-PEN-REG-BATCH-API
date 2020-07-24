package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author OM
 * The type Student details.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentDetails {
  private String transactionCode; // TRANSACTION_CODE	3	0	Always "SRM"
  private String localStudentID; //LOCAL_STUDENT_ID	12	3	The student identifier that is assigned by the school.
  private String pen; // PEN	10	15	The Personal Education Number that is assigned by the Ministry. Left-justified, consisting of 9 digits followed by a blank.
  private String legalSurname; // LEGAL_SURNAME	25	25	The legal name is the name appearing on the student's birth certificate or a change of name document.
  private String legalGivenName; // LEGAL_GIVEN_NAME	25	50	The first or given legal name of the student.
  private String legalMiddleName; // LEGAL_MIDDLE_NAME	25	75	The second or middle legal name of the student.
  private String usualSurname; // USUAL_SURNAME	25	100	The surname that the student prefers to be known by.
  private String usualGivenName; // USUAL_GIVEN_NAME	25	125	The first or given name that a student prefers to be known by.
  private String usualMiddleName; // USUAL_MIDDLE_NAME	25	150	The second or middle name that the student prefers to be known by.
  private String birthDate; //  BIRTH DATE	8	175	The birth date of the student. Format: YYYYMMDD
  private String gender; // GENDER	1	183	The gender of the student (M or F)
  private String unused; // UNUSED	16	184
  private String enrolledGradeCode; // ENROLLED_GRADE_CODE	2	200	Must be HS, KH, KF, 01, 02, 03, 04, 05, 06, 07, EU, 08, 09, 10, 11, 12, SU, GA.
  private String unusedSecond; //  UNUSED	26	202
  private String postalCode; // POSTAL_CODE	6	228	Student's British Columbia postal code

}
