package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.Tuple;

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
  Page<Tuple> findByAttributesAndPenRequestBatchStudent(Specification<PenRequestBatchEntity> specification, Pageable pageable);

}
