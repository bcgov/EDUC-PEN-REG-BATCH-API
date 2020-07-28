package ca.bc.gov.educ.penreg.api.batch.constants;

import lombok.Getter;

@Getter
public enum PenRequestBatchTypeCode {
  SCHOOL("SCHOOL"), NOM_ROLL("NOMROLL"), SLD("SLD");

  private final String code;

  PenRequestBatchTypeCode(String code) {
    this.code = code;
  }
}
