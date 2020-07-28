package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

public enum PenRequestBatchStatusCodes {
  NEW("NEW"), LOAD_FAIL("LOADFAIL"), LOADED("LOADED");

  @Getter
  private final String code;

  PenRequestBatchStatusCodes(String code) {
    this.code = code;
  }
}
