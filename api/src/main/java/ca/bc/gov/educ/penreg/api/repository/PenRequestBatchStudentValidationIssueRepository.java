package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentValidationIssueEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PenRequestBatchStudentValidationIssueRepository extends CrudRepository<PenRequestBatchStudentValidationIssueEntity, UUID> {
}
