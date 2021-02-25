package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.MinistryPRBSourceCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Ministry prb source code repository.
 */
@Repository
public interface MinistryPRBSourceCodeRepository extends CrudRepository<MinistryPRBSourceCodeEntity, String> {
}
