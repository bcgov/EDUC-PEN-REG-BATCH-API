package ca.bc.gov.educ.penreg.api.struct.v1.external;

import java.util.List;
import lombok.Data;

@Data
public class PenRequestBatchSubmissionResult {
  List<ListItem> pendingList;
  List<ListItem> newPenAssignedList;
  List<ListItem> exactMatchList;
  List<SchoolMinListItem> differencesList;
  List<SchoolMinListItem> confirmedList;
}
