package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PenRequestBatchArchivedEmailEvent {
    String fromEmail;
    String toEmail;
    String submissionNumber;
    String schoolName;
    String mincode;
    PendingRecords pendingRecords;
}
