package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Pen request batch repository.
 */
@Repository
public interface PenRequestBatchRepository extends CrudRepository<PenRequestBatchEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchEntity> {

  List<PenRequestBatchEntity> findAll();

  /**
   * Find by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  Optional<PenRequestBatchEntity> findBySubmissionNumber(String submissionNumber);
}
