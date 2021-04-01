package ca.bc.gov.educ.penreg.api.struct.v1.reportstructs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportListItem {
    String pen;
    String surname;
    String givenName;
    String legalMiddleNames;
    String birthDate;
    String gender;
    String schoolID;
    String usualName;
    String reason;
}
