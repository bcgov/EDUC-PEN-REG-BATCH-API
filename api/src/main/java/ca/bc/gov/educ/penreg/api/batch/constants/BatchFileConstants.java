package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

/**
 * The enum Batch file constants.
 * @author OM
 */
public enum BatchFileConstants {
  /**
   * Transaction code batch file constants.
   */
  TRANSACTION_CODE("transactionCode"),
  /**
   * Min code batch file constants.
   */
  MIN_CODE("minCode"),
  /**
   * School name batch file constants.
   */
  SCHOOL_NAME("schoolName"),
  /**
   * Request date batch file constants.
   */
  REQUEST_DATE("requestDate"),
  /**
   * Email batch file constants.
   */
  EMAIL("emailID"),
  /**
   * Fax number batch file constants.
   */
  FAX_NUMBER("faxNumber"),
  /**
   * Contact name batch file constants.
   */
  CONTACT_NAME("contactName"),
  /**
   * Office number batch file constants.
   */
  OFFICE_NUMBER("officeNumber"),
  /**
   * Local student id batch file constants.
   */
  LOCAL_STUDENT_ID("localStudentID"),
  /**
   * Pen batch file constants.
   */
  PEN("pen"),
  /**
   * Legal surname batch file constants.
   */
  LEGAL_SURNAME("legalSurname"),
  /**
   * Legal given name batch file constants.
   */
  LEGAL_GIVEN_NAME("legalGivenName"),
  /**
   * Legal middle name batch file constants.
   */
  LEGAL_MIDDLE_NAME("legalMiddleName"),
  /**
   * Usual surname batch file constants.
   */
  USUAL_SURNAME("usualSurname"),
  /**
   * Usual given name batch file constants.
   */
  USUAL_GIVEN_NAME("usualGivenName"),
  /**
   * Usual middle name batch file constants.
   */
  USUAL_MIDDLE_NAME("usualMiddleName"),
  /**
   * Birth date batch file constants.
   */
  BIRTH_DATE("birthDate"),
  /**
   * Gender batch file constants.
   */
  GENDER("gender"),
  /**
   * Unused batch file constants.
   */
  UNUSED("unused"),
  /**
   * Enrolled grade code batch file constants.
   */
  ENROLLED_GRADE_CODE("enrolledGradeCode"),
  /**
   * Unused second batch file constants.
   */
  UNUSED_SECOND("unusedSecond"),
  /**
   * Postal code batch file constants.
   */
  POSTAL_CODE("postalCode"),
  /**
   * Student count batch file constants.
   */
  STUDENT_COUNT("studentCount"),
  /**
   * Vendor name batch file constants.
   */
  VENDOR_NAME("vendorName"),
  /**
   * Product name batch file constants.
   */
  PRODUCT_NAME("productName"),
  /**
   * Product id batch file constants.
   */
  PRODUCT_ID("productID"),
  /**
   * Header batch file constants.
   */
  HEADER("header"),
  /**
   * Trailer batch file constants.
   */
  TRAILER("trailer");
  @Getter
  private final String name;
  BatchFileConstants(String name) {
    this.name = name;
  }
}
