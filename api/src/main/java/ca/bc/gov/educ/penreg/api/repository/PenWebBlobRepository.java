package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Pen web blob repository.
 */
@Repository
public interface PenWebBlobRepository extends CrudRepository<PENWebBlobEntity, Long> {
  /**
   * Find all by extract date time is null list.
   *
   * @return the list
   */
  List<PENWebBlobEntity> findAllByExtractDateTimeIsNullAndFileType(String fileType);
}
