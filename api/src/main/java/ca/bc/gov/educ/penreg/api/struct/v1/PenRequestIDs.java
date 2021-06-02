package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestIDs {
  UUID penRequestBatchStudentID;
  UUID penRequestBatchID;
}
