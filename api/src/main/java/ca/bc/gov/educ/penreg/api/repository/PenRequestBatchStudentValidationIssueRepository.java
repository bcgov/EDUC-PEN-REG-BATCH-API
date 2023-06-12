package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch student validation issue repository.
 */
@Repository
public interface PenRequestBatchStudentValidationIssueRepository extends CrudRepository<PenRequestBatchStudentValidationIssueEntity, UUID> {
  List<PenRequestBatchStudentValidationIssueEntity> findByPenRequestBatchStudentEntity_penRequestBatchEntity(PenRequestBatchEntity penRequestBatch);

  List<PenRequestBatchStudentValidationIssueEntity> findAllByPenRequestBatchStudentEntity(PenRequestBatchStudentEntity penRequestBatchStudentEntity);

  List<PenRequestBatchStudentValidationIssueEntity> findAllByPenRequestBatchStudentEntity_penRequestBatchStudentIDIn(List<UUID> penRequestBatchStudentIDs);
}
