package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
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

  String errorMessage;
}
