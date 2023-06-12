package ca.bc.gov.educ.penreg.api.struct.v1;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestIDs {
  UUID penRequestBatchStudentID;
  UUID penRequestBatchID;
}
