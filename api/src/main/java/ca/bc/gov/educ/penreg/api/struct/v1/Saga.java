package ca.bc.gov.educ.penreg.api.struct.v1;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Saga.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Saga {
  UUID sagaId;
  UUID penRequestBatchStudentID;
  UUID penRequestBatchID;
  String sagaName;
  String sagaState;
  String payload;
  String status;
  String createUser;
  String updateUser;
  String createDate;
  String updateDate;
  Integer retryCount;
}
