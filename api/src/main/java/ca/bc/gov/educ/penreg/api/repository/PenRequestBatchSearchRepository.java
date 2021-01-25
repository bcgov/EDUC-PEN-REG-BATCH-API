package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

/**
 * The interface Pen request batch search repository.
 */
public interface PenRequestBatchSearchRepository {

  /**
   * Find by pen request batch student.
   *
   * @param specification   the specification criteria
   * @param pageable   the page and sort criteria
   * @return the page
   */
  Page<Pair<PenRequestBatchEntity, Long>> findByPenRequestBatchStudent(@NonNull Specification<PenRequestBatchEntity> specification, Pageable pageable);

}
