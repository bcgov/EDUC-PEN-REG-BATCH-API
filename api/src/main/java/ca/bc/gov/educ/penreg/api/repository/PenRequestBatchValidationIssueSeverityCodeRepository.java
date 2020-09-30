package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchValidationIssueSeverityCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenRequestBatchValidationIssueSeverityCodeRepository extends CrudRepository<PenRequestBatchValidationIssueSeverityCodeEntity, String> {
}
