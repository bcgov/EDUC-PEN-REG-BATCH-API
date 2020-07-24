package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

public enum BatchFileConstants {
  TRANSACTION_CODE("transactionCode"),
  MIN_CODE("minCode"),
  SCHOOL_NAME("schoolName"),
  REQUEST_DATE("requestDate"),
  EMAIL("emailID"),
  FAX_NUMBER("faxNumber"),
  CONTACT_NAME("contactName"),
  OFFICE_NUMBER("officeNumber"),
  LOCAL_STUDENT_ID("localStudentID"),
  PEN("pen"),
  LEGAL_SURNAME("legalSurname"),
  LEGAL_GIVEN_NAME("legalGivenName"),
  LEGAL_MIDDLE_NAME("legalMiddleName"),
  USUAL_SURNAME("usualSurname"),
  USUAL_GIVEN_NAME("usualGivenName"),
  USUAL_MIDDLE_NAME("usualMiddleName"),
  BIRTH_DATE("birthDate"),
  GENDER("gender"),
  UNUSED("unused"),
  ENROLLED_GRADE_CODE("enrolledGradeCode"),
  UNUSED_SECOND("unusedSecond"),
  POSTAL_CODE("postalCode"),
  STUDENT_COUNT("studentCount"),
  VENDOR_NAME("vendorName"),
  PRODUCT_NAME("productName"),
  PRODUCT_ID("productID"),
  HEADER("header"),
  TRAILER("trailer");
  @Getter
  private final String name;
  BatchFileConstants(String name) {
    this.name = name;
  }
}
