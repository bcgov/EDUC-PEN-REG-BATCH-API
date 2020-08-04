package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.UnarchivedBatchStatusCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Unarchived batch status code repository.
 */
@Repository
public interface UnarchivedBatchStatusCodeRepository extends CrudRepository<UnarchivedBatchStatusCodeEntity, String> {
}
