package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentStatusCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch student status code repository.
 */
@Repository
public interface PenRequestBatchStudentStatusCodeRepository extends CrudRepository<PenRequestBatchStudentStatusCodeEntity, String> {
}
