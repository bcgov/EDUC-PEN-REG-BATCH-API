package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PenRequestBatchStat {
  Long fixableCount;
  Long repeatCount;
  Long pendingCount;
  Long unarchivedCount;
  Long heldForReviewCount; // this includes (DUPLICATE, HOLD_SIZE)
  String schoolGroupCode; // K-12 or PSI
}
