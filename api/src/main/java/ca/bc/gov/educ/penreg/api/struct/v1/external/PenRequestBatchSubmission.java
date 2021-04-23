package ca.bc.gov.educ.penreg.api.struct.v1.external;

import ca.bc.gov.educ.penreg.api.struct.v1.BasePenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class PenRequestBatchSubmission extends BasePenRequestBatch {

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<PenRequestBatchStudent> students;
}


