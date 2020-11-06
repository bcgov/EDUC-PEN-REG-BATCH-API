package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;

import java.util.UUID;

public abstract class BaseOrchestratorTest {
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
   * The Mincode.
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
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \""+ mincode + "\",\n" +
        "    \"genderCode\": \"X\",\n" +
        "    \"twinStudentIDs\": [\"" + twinStudentID + "\"]\n" +
        "  }";
  }

  /**
   * Gets pen request batch new pen saga data from json string.
   *
   * @param json the json
   * @return the pen request batch new pen saga data from json string
   */
  protected PenRequestBatchUserActionsSagaData getPenRequestBatchUserActionsSagaDataFromJsonString(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestBatchUserActionsSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
