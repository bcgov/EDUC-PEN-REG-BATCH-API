package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The interface Pen request batch repository.
 */
@Repository
public interface PenRequestBatchRepository extends JpaRepository<PenRequestBatchEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchEntity>, PenRequestBatchSearchRepository {

  /**
   * Find by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  List<PenRequestBatchEntity> findBySubmissionNumber(String submissionNumber);

  /**
   * Find by pen request batch status code list.
   *
   * @param penRequestBatchStatusCode the pen request batch status code
   * @return the list
   */
  List<PenRequestBatchEntity> findByPenRequestBatchStatusCode(String penRequestBatchStatusCode);

  List<PenRequestBatchEntity> findByPenRequestBatchStatusCodeAndSchoolGroupCode(String penRequestBatchStatusCode, String schoolGroupCode);

  long countPenRequestBatchEntitiesByPenRequestBatchStatusCode(String penRequestBatchStatusCode);

  long countAllByPenRequestBatchStatusCodeInAndSchoolGroupCode(List<String> penRequestBatchStatusCodes, String schoolGroupCode);

  List<PenRequestBatchEntity> findByPenRequestBatchStatusCodeAndCreateDateBefore(String penRequestBatchStatusCode, LocalDateTime createDate);
  List<PenRequestBatchEntity> findAllByPenRequestBatchStatusCodeOrderByCreateDate(String penRequestBatchStatusCode);
}
