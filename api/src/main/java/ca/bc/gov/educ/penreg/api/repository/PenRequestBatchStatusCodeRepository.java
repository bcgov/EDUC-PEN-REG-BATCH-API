package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStatusCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch status code repository.
 */
@Repository
public interface PenRequestBatchStatusCodeRepository extends CrudRepository<PenRequestBatchStatusCodeEntity, String> {
}
