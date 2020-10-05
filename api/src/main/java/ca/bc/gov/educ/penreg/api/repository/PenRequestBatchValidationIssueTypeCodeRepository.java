package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchValidationIssueTypeCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch validation issue type code repository.
 */
@Repository
public interface PenRequestBatchValidationIssueTypeCodeRepository extends CrudRepository<PenRequestBatchValidationIssueTypeCodeEntity, String> {
}
