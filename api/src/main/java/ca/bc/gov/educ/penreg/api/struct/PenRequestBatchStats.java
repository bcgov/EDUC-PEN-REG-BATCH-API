package ca.bc.gov.educ.penreg.api.struct;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestBatchStats {
  List<PenRequestBatchStat> penRequestBatchStatList;
  Long loadFailCount;
}
