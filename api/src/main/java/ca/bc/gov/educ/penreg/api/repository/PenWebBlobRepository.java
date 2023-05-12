package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
   * Find all Pen Web files by extract date time is null list.
   *
   * @param fileType the file type
   * @return the list
   */
  List<PENWebBlobEntity> findAllByExtractDateTimeIsNullAndFileTypeAndSourceApplication(String fileType, String sourceApplication);

  /**
   * Find all by submission number and file type.
   *
   * @param submissionNumber the submission number
   * @param fileType         the file type, such as `PEN`
   * @return the list
   */
  List<PENWebBlobEntity> findAllBySubmissionNumberAndFileType(String submissionNumber, String fileType);

  /**
   * Find all by submission number.
   *
   * @param submissionNumber the submission number
   * @return the list
   */
  List<PENWebBlobEntity> findAllBySubmissionNumber(String submissionNumber);

  List<PENWebBlobEntity> findAllByMincodeAndInsertDateTimeGreaterThanAndSubmissionNumberNotAndFileTypeAndExtractDateTimeIsNotNull(String mincode, LocalDateTime insertDateTime, String submissionNumber, String fileType);
}
