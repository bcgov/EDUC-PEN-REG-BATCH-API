package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PenRequestBatchArchiveAndReturnSagaData extends BaseRequest {

    /**
     * The pen request batch id.
     */
    @NotNull(message = "penRequestBatchID cannot be null")
    UUID penRequestBatchID;
}
