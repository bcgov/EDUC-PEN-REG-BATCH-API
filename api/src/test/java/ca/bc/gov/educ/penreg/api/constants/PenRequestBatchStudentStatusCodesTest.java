package ca.bc.gov.educ.penreg.api.constants;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PenRequestBatchStudentStatusCodesTest {

  @Test
  public void valueOfCode() {
    assertThat(PenRequestBatchStudentStatusCodes.valueOfCode("NEWPENUSR")).isNotNull().isEqualTo(PenRequestBatchStudentStatusCodes.USR_NEW_PEN);
  }
}
