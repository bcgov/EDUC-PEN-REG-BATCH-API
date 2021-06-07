package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * The interface Pen request batch student validation issue repository.
 */
@Repository
public interface PenRequestBatchStudentValidationIssueRepository extends CrudRepository<PenRequestBatchStudentValidationIssueEntity, UUID> {
  List<PenRequestBatchStudentValidationIssueEntity> findByPenRequestBatchStudentEntity_penRequestBatchEntity(PenRequestBatchEntity penRequestBatchID);
}
