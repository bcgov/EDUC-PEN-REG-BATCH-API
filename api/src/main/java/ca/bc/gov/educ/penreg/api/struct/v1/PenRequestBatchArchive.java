package ca.bc.gov.educ.penreg.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PenRequestBatchArchive extends BaseRequest {

    /**
     * The pen request batch id.
     */
    @NotNull(message = "penRequestBatchID cannot be null")
    UUID penRequestBatchID;
}
