package ca.bc.gov.educ.penreg.api.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@RunWith(JUnitParamsRunner.class)
@ActiveProfiles("test")
@Slf4j
public class PenRequestBatchStudentOrchestratorServiceTest {
  @ClassRule
  public static final SpringClassRule scr = new SpringClassRule();

  @Rule
  public final SpringMethodRule smr = new SpringMethodRule();

  @Autowired
  private PenRequestBatchStudentOrchestratorService orchestratorService;

  @Test
  @Parameters({
      "null, null, true",
      ",, true",
      "hi,hi, true",
      "hello,hello, true",
      "hello,hi, false",
  })
  public void testAreBothFieldValueEqual(String field1, String field2, boolean res) {
    if ("null".equals(field1)) {
      field1 = null;
    }
    if ("null".equals(field2)) {
      field2 = null;
    }
    var result = orchestratorService.areBothFieldValueEqual(field1, field2);
    assertThat(result).isEqualTo(res);

  }

  @Test
  @Parameters({
      "a,A",
      "a  ,A",
      "hi\t,HI",
      "hello.,HELLO",
      "  he    llo      ,HE LLO",
  })
  public void scrubNameField(String fieldValue, String scrubbedValue) {
    var result = orchestratorService.scrubNameField(fieldValue);
    assertThat(result).isEqualTo(scrubbedValue);
  }
}