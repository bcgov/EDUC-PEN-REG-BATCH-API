package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.struct.Student;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

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
