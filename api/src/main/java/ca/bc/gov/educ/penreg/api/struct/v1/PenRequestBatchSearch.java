package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Pen request batch search.
 *
 * @author OM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S1700")
public class PenRequestBatchSearch extends BasePenRequestBatch {
  /**
   * The count of searched student records
   */
  Long searchedCount;
}
