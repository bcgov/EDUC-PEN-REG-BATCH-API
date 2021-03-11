package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PenRequestBatchArchiveAndReturnAllSagaData extends BaseRequest {

    /**
     * The List of pen request batch ids.
     */
    @NotNull(message = "penRequestBatchIDs cannot be null")
    List<UUID> penRequestBatchIDs;
}
