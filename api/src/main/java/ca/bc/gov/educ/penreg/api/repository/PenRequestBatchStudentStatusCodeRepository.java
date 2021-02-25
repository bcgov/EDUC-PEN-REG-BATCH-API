package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentStatusCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Pen request batch student status code repository.
 */
@Repository
public interface PenRequestBatchStudentStatusCodeRepository extends CrudRepository<PenRequestBatchStudentStatusCodeEntity, String> {

  /**
   * Find all list.
   *
   * @return the list
   */
  @Override
  List<PenRequestBatchStudentStatusCodeEntity> findAll();
}
