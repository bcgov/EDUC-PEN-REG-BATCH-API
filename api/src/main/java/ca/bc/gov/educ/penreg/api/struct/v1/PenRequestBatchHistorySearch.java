package ca.bc.gov.educ.penreg.api.struct.v1;

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
public class PenRequestBatchHistorySearch extends BasePenRequestBatch {
}
