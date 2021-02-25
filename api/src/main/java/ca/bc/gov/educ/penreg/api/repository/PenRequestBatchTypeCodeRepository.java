package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchTypeCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch type code repository.
 */
@Repository
public interface PenRequestBatchTypeCodeRepository extends CrudRepository<PenRequestBatchTypeCodeEntity, String> {
}
