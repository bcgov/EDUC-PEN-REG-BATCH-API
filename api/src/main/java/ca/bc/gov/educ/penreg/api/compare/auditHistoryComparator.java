package ca.bc.gov.educ.penreg.api.compare;

import ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode;
import ca.bc.gov.educ.penreg.api.struct.v1.StudentHistory;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;


/**
 * This custom comparator will sort audit history by DESC date order and addresses ticket HD-11028.
 * If dates are equal then we will force history activity code REQ_MATCH to the top of the list and avoid index out of bounds error.
 */
public class auditHistoryComparator implements Comparator<StudentHistory> {
  @Override
  public int compare(StudentHistory x, StudentHistory y) {
    int dateResult = y.getCreateDate().compareTo(x.getCreateDate());

    if (dateResult == 0) {
      return StringUtils.equals(x.getHistoryActivityCode(), StudentHistoryActivityCode.REQ_MATCH.getCode()) ? -1 : 0;
//      Boolean result = StringUtils.equals(y.getHistoryActivityCode(), StudentHistoryActivityCode.REQ_MATCH.getCode());
//      if (result) {
//        return 1;
//      }
//      return -1;
    }

    return dateResult;
  }
}
