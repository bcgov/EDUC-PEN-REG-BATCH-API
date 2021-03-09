package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Pen request batch.
 *
 * @author OM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@SuppressWarnings("squid:S1700")
public class PenRequestBatchHistory extends BasePenRequestBatch {

    /**
     * The Pen request batch history id.
     */
    String penRequestBatchHistoryID;
}
