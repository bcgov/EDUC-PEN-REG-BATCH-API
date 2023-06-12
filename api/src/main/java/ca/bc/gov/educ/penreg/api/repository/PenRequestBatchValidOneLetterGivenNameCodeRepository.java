package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchValidOneLetterGivenNameCodeEntity;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch valid one letter given name code repository.
 */
@Repository
public interface PenRequestBatchValidOneLetterGivenNameCodeRepository extends CrudRepository<PenRequestBatchValidOneLetterGivenNameCodeEntity, String> {

  /**
   * Find all list.
   *
   * @return the list
   */
  @Override
  List<PenRequestBatchValidOneLetterGivenNameCodeEntity> findAll();
}
