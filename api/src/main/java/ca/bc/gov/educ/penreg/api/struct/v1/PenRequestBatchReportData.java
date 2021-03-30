package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
