package ca.bc.gov.educ.penreg.api.struct;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Pen request batch student unmatch saga data.
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestBatchUnmatchSagaData extends BasePenRequestBatchStudentSagaData {
  /**
   * The Twins
   */
  List<String> matchedStudentIDList;
}
