package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The interface Pen web blob repository.
 */
@Repository
public interface PenWebBlobRepository extends JpaRepository<PENWebBlobEntity, Long> {
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

  List<PENWebBlobEntity> findAllByMincodeAndInsertDateTimeGreaterThanAndSubmissionNumberNotAndFileTypeAndExtractDateTimeIsNotNull(String mincode, LocalDateTime insertDateTime, String submissionNumber, String fileType);
}
