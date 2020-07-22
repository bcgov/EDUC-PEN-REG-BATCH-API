package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.StatusCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Gender Code Table Repository
 *
 * @author Marco Villeneuve
 * 
 */
@Repository
public interface StatusCodeTableRepository extends CrudRepository<StatusCodeEntity, Long> {
    List<StatusCodeEntity> findAll();
}
