package ca.bc.gov.educ.penreg.api.struct.v1.reportstructs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportGenerationEvent {
    String reportType;
    String reportExtension;
    String reportName;
    PenRequestBatchReportData data;
}
