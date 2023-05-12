package ca.bc.gov.educ.penreg.api.struct.v1.reportstructs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PenRequestBatchReportData {
    String processDate;
    String processTime;
    String submissionNumber;
    String reportDate;
    String reviewer;
    String mincode;
    String schoolName;
    String penCordinatorEmail;
    String mailingAddress;
    String telephone;
    String fascimile;
    List<ReportListItem> pendingList;
    List<ReportListItem> newPenList;
    List<ReportListItem> sysMatchedList;
    List<ReportUserMatchedListItem> diffList;
    List<ReportUserMatchedListItem> confirmedList;
}
