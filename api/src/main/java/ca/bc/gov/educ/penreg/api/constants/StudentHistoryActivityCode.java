package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

public enum StudentHistoryActivityCode {
  REQ_MATCH("REQMATCH"),
  USER_NEW("USERNEW"),
  REQ_NEW("REQNEW"),
  REQ_UNMATCH("REQUNMATCH");

  @Getter
  private final String code;

  StudentHistoryActivityCode(String code) {
    this.code = code;
  }
}
