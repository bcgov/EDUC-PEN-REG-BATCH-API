package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.SchoolGroupCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface School group code repository.
 */
@Repository
public interface SchoolGroupCodeRepository extends CrudRepository<SchoolGroupCodeEntity, String> {
}
