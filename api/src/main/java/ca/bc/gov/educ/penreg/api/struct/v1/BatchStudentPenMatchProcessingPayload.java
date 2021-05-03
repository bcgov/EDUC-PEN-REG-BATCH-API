package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * place holder for objects during batch student saga process.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchStudentPenMatchProcessingPayload {
  Saga saga;
  PenRequestBatchStudentSagaData penRequestBatchStudentSagaData;
  PenMatchResult penMatchResult;
  PenRequestBatchStudentEntity penRequestBatchStudentEntity;
  PenRequestBatchEntity penRequestBatchEntity;
}
