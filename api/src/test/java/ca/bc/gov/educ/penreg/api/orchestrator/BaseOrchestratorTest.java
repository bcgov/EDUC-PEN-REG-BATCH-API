package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.BaseTest;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;

import java.util.UUID;

public abstract class BaseOrchestratorTest extends BaseTest {
  protected static final String TEST_PEN = "123456789";
  /**
   * The Pen request batch id.
   */
  protected final String penRequestBatchID = UUID.randomUUID().toString();

  /**
   * The Pen request batch student id.
   */
  protected final String penRequestBatchStudentID = UUID.randomUUID().toString();

  /**
   * The Twin student id.
   */
  protected final String twinStudentID = UUID.randomUUID().toString();

  /**
   * The mincode.
   */
  protected final String mincode = "01292001";
  /**
   * Dummy pen request batch new pen saga data json string.
   *
   * @return the string
   */
  protected String placeholderPenRequestBatchActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \"" + this.mincode + "\",\n" +
        "    \"genderCode\": \"X\",\n" +
        "    \"matchedStudentIDList\": [\"" + this.twinStudentID + "\"]\n" +
        "  }";
  }

  /**
   * Gets pen request batch new pen saga data from json string.
   *
   * @param json the json
   * @return the pen request batch new pen saga data from json string
   */
  protected PenRequestBatchUserActionsSagaData getPenRequestBatchUserActionsSagaDataFromJsonString(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestBatchUserActionsSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
