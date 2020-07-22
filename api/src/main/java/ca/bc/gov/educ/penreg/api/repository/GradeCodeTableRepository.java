package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.GradeCodeEntity;
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
public interface GradeCodeTableRepository extends CrudRepository<GradeCodeEntity, Long> {
    List<GradeCodeEntity> findAll();
}
