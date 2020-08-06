package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The interface Pen web blob repository.
 */
@Repository
public interface PenWebBlobRepository extends CrudRepository<PENWebBlobEntity, Long> {
  /**
   * Find all by extract date time is null list.
   *
   * @param fileType the file type
   * @return the list
   */
  List<PENWebBlobEntity> findAllByExtractDateTimeIsNullAndFileType(String fileType);

  /**
   * Find by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  Optional<PENWebBlobEntity> findBySubmissionNumber(String submissionNumber);
}
