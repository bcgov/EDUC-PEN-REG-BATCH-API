package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArchiveAndReturnSagaResponse {
    /**
     * The pen request batch id.
     */
    UUID penRequestBatchID;
    /**
     * The saga id.
     */
    UUID sagaId;
}
