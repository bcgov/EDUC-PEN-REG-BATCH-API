package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Place holder for objects during one of pen request from external clients.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestPenMatchProcessingPayload {
  PenRequest penRequest;
  PenMatchResult penMatchResult;
  PenRequestResult penRequestResult;
  UUID transactionID;
}
