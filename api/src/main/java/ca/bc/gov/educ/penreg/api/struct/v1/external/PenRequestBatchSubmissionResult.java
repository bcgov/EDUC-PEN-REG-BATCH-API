package ca.bc.gov.educ.penreg.api.struct.v1.external;

import lombok.Data;

import java.util.List;

@Data
public class PenRequestBatchSubmissionResult {
  List<ListItem> pendingList;
  List<ListItem> newPenAssignedList;
  List<ListItem> exactMatchList;
  List<SchoolMinListItem> differencesList;
  List<SchoolMinListItem> confirmedList;
}
