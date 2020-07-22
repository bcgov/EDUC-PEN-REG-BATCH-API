package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.SexCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Sex Code Table Repository
 *
 * @author Marco Villeneuve
 * 
 */
@Repository
public interface SexCodeTableRepository extends CrudRepository<SexCodeEntity, Long> {
    List<SexCodeEntity> findAll();
}
