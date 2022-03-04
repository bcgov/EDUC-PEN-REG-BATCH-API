package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  /**
   * Find all pen numbers that were issued to more than one student in pen batch.
   *
   * @param  penRequestBatchID the pen request batch ID
   * @return a list of PEN numbers that were assigned to more than one student
   */
  @Query(value = "select PEN_REQUEST_BATCH_STUDENT_ID\n"
      + "from PEN_REQUEST_BATCH_STUDENT\n"
      + "where STUDENT_ID in (\n"
      + "SELECT\n"
      + "    student_id\n"
      + "FROM\n"
      + "    PEN_REQUEST_BATCH_STUDENT\n"
      + "WHERE PEN_REQUEST_BATCH_ID in :penRequestBatchIDs\n"
      + "GROUP BY\n"
      + "    student_id\n"
      + "HAVING\n"
      + "    COUNT( student_id ) > 1)\n"
      + "AND PEN_REQUEST_BATCH_ID in :penRequestBatchIDs",
    nativeQuery = true)
  List<String> findSameAssignedPensByPenRequestBatchID(@Param("penRequestBatchIDs") List<UUID> penRequestBatchID);

  List<PenRequestBatchEntity> findByPenRequestBatchStatusCodeAndSchoolGroupCode(String penRequestBatchStatusCode, String schoolGroupCode);

  long countPenRequestBatchEntitiesByPenRequestBatchStatusCode(String penRequestBatchStatusCode);

  long countAllByPenRequestBatchStatusCodeInAndSchoolGroupCode(List<String> penRequestBatchStatusCodes, String schoolGroupCode);

  List<PenRequestBatchEntity> findByPenRequestBatchStatusCodeAndCreateDateBefore(String penRequestBatchStatusCode, LocalDateTime createDate);


  List<PenRequestBatchEntity> findTop100ByPenRequestBatchStatusCodeOrderByCreateDate(String penRequestBatchStatusCode);

  List<PenRequestBatchEntity> findTop10ByPenRequestBatchStatusCodeAndCreateDateBeforeOrderByCreateDate(String penRequestBatchStatusCode, LocalDateTime createDate);
}
