package ca.bc.gov.educ.penreg.api.compare;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import java.util.Comparator;

/**
 * The type New pen match comparator.
 */
public class PenRequestBatchHistoryComparator implements Comparator<PenRequestBatchHistoryEntity> {
  @Override
  public int compare(PenRequestBatchHistoryEntity x, PenRequestBatchHistoryEntity y) {
    //This is a single result situation...no algorithm was used
    if (x.getCreateDate() == null) {
      return 0;
    }

    if (x.getCreateDate().isAfter(y.getCreateDate())) {
      return 1;
    }

    return -1;
  }
}
