package ca.bc.gov.educ.penreg.api.compare;

import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.REQ_NEW;
import static ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode.USER_NEW;
import static org.assertj.core.api.Assertions.assertThat;

import ca.bc.gov.educ.penreg.api.struct.v1.StudentHistory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

public class PenRequestBatchHistoryComparatorTest {
  @Test
  public void testRevertStudentInformationComparator_givenSameCreateDate_shouldPutReqMatchFirst() throws IOException, InterruptedException, TimeoutException {

    List<StudentHistory> studentAuditHistoryWithSameCreateDate = new ArrayList<>();
    studentAuditHistoryWithSameCreateDate.add(studentAuditHistoryCreatorForRevertStudentInformationTest("second", "", "", "","","","", REQ_NEW.getCode(), "22-02-02"));
    studentAuditHistoryWithSameCreateDate.add(studentAuditHistoryCreatorForRevertStudentInformationTest("third", "", "", "","","","", USER_NEW.getCode(), "22-02-02"));
    studentAuditHistoryWithSameCreateDate.add(studentAuditHistoryCreatorForRevertStudentInformationTest("first", "", "", "","","","", REQ_MATCH.getCode(), "22-02-02"));

    Collections.sort(studentAuditHistoryWithSameCreateDate, new auditHistoryComparator());

    //check that the ordering of student history is correct. REQ_MATCH moves to the top and other elements remain the same.
    assertThat(studentAuditHistoryWithSameCreateDate.get(0).getUsualFirstName()).isEqualTo("first");
    assertThat(studentAuditHistoryWithSameCreateDate.get(1).getUsualFirstName()).isEqualTo("second");
    assertThat(studentAuditHistoryWithSameCreateDate.get(2).getUsualFirstName()).isEqualTo("third");
  }

  /**
   *
   *@param usualFirstName usual first name
   *@param usualMiddleName usual middle name
   *@param usualLastName usual last name
   *@param historyActivityCode history activity code (i.e. REQ_MATCH)
   *@param createDate the create date
   *
   * @return the student's audit history
   */
  private StudentHistory studentAuditHistoryCreatorForRevertStudentInformationTest(final String usualFirstName, final String usualMiddleName, final String usualLastName, final String localID, final String gradeCode, final String gradeYear,  final String postalCode, final String historyActivityCode, final String createDate) {
    return StudentHistory.builder()
        .usualFirstName(usualFirstName)
        .usualMiddleNames(usualMiddleName)
        .usualLastName(usualLastName)
        .localID(localID)
        .gradeCode(gradeCode)
        .gradeYear(gradeYear)
        .postalCode(postalCode)
        .historyActivityCode(historyActivityCode)
        .createDate(createDate)
        .build();
  }

}
