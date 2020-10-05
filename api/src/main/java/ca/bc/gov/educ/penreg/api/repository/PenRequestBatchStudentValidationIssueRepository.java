package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentValidationIssueEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The interface Pen request batch student validation issue repository.
 */
@Repository
public interface PenRequestBatchStudentValidationIssueRepository extends CrudRepository<PenRequestBatchStudentValidationIssueEntity, UUID> {
}
