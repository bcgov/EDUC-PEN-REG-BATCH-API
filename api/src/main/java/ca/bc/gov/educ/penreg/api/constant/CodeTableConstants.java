package ca.bc.gov.educ.penreg.api.constant;

public enum CodeTableConstants {
  DATA_SOURCE_API_BASE_PATH("/datasource-codes"),
  GENDER_CODE_API_BASE_PATH("/gender-codes");

  private final String basePath;

  CodeTableConstants(final String basePath) {
    this.basePath = basePath;
  }

  public String getValue() {
    return this.basePath;
  }
}
