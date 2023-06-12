package ca.bc.gov.educ.penreg.api.struct;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStat;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestBatchStats {
  List<PenRequestBatchStat> penRequestBatchStatList;
  Long loadFailCount;
}
