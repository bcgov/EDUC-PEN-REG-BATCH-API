package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The interface Pen request batch student repository.
 */
public interface PenRequestBatchStudentRepository extends CrudRepository<PenRequestBatchStudentEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchStudentEntity> {

  /**
   * Find all list.
   *
   * @return the list
   */
  @Override
  List<PenRequestBatchStudentEntity> findAll();

  /**
   * Find all by pen request batch entity list.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the list
   */
  List<PenRequestBatchStudentEntity> findAllByPenRequestBatchEntity(PenRequestBatchEntity penRequestBatchEntity);

  /**
   * Find all by pen request batch entity and pen request batch student status code is not list.
   *
   * @param penRequestBatchEntity             the pen request batch entity
   * @param penRequestBatchStudentStatusCodes the pen request batch student status codes
   * @return the list
   */
  List<PenRequestBatchStudentEntity> findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsIn(PenRequestBatchEntity penRequestBatchEntity, List<String> penRequestBatchStudentStatusCodes);

  /**
   * Find all pen request batch student entities that are repeats given the input parameters
   *
   * @param mincode                           the mincode of pen request batch entity
   * @param penRequestBatchStatusCode         the penRequestBatchStatusCode of pen request batch entity
   * @param startDate                         the earliest process of pen request batch entity
   * @param localID                           the localID of the pen request batch student entity
   * @param penRequestBatchStudentStatusCodes the allowed penRequestBatchStudentStatusCodes
   * @param submittedPen                      the submittedPen of the pen request batch student entity
   * @param legalFirstName                    the legalFirstName of the pen request batch student entity
   * @param legalMiddleNames                  the legalMiddleNames of the pen request batch student entity
   * @param legalLastName                     the legalLastName of the pen request batch student entity
   * @param usualFirstName                    the usualFirstName of the pen request batch student entity
   * @param usualMiddleNames                  the usualMiddleNames of the pen request batch student entity
   * @param usualLastName                     the usualLastName of the pen request batch student entity
   * @param dob                               the dob of the pen request batch student entity
   * @param genderCode                        the genderCode of the pen request batch student entity
   * @param gradeCode                         the gradeCode of the pen request batch student entity
   * @param postalCode                        the postalCode of the pen request batch student entity
   * @return the list
   */
  @Query("select t from PenRequestBatchStudentEntity t WHERE t.penRequestBatchEntity IN (select s from PenRequestBatchEntity s WHERE s.mincode = :mincode AND s.penRequestBatchStatusCode = :penRequestBatchStatusCode AND s.processDate >= :startDate) AND (:localID is null or t.localID = :localID) AND t.penRequestBatchStudentStatusCode NOT IN :penRequestBatchStudentStatusCodes AND  (:submittedPen is null or t.submittedPen = :submittedPen) AND (:legalFirstName is null or t.legalFirstName = :legalFirstName) AND (:legalMiddleNames is null or t.legalMiddleNames = :legalMiddleNames) AND (:legalLastName is null or t.legalLastName = :legalLastName) AND (:usualFirstName is null or t.usualFirstName = :usualFirstName) AND (:usualMiddleNames is null or t.usualMiddleNames = :usualMiddleNames) AND (:usualLastName is null or t.usualLastName = :usualLastName) AND (:dob is null or t.dob = :dob) AND (:genderCode is null or t.genderCode = :genderCode) AND (:gradeCode is null or t.gradeCode = :gradeCode) AND (:postalCode is null or t.postalCode = :postalCode)")
  List<PenRequestBatchStudentEntity> findAllRepeatsGivenBatchStudent(@Param("mincode") String mincode, @Param("penRequestBatchStatusCode") String penRequestBatchStatusCode, @Param("startDate") LocalDateTime startDate, @Param("localID") String localID, List<String> penRequestBatchStudentStatusCodes, String submittedPen, String legalFirstName, String legalMiddleNames, String legalLastName, String usualFirstName, String usualMiddleNames, String usualLastName, String dob, String genderCode, String gradeCode, String postalCode);


  /**
   * Finds all pen request batch student entities given a pen request batch entity, local id is not null, and pen request batch student status code is in list
   *
   * @param penRequestBatchEntity             - the pen request batch entity
   * @param penRequestBatchStudentStatusCodes - the list of pen request batch student status codes
   * @return - the list
   */
  List<PenRequestBatchStudentEntity> findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsInAndLocalIDNotNull(PenRequestBatchEntity penRequestBatchEntity, List<String> penRequestBatchStudentStatusCodes);

  @Query("select t from PenRequestBatchStudentEntity t WHERE t.penRequestBatchEntity IN (select s from PenRequestBatchEntity s WHERE s.mincode = :mincode AND s.penRequestBatchStatusCode = :penRequestBatchStatusCode AND s.processDate >= :startDate) AND t.penRequestBatchStudentStatusCode NOT IN :penRequestBatchStudentStatusCodes")
  List<PenRequestBatchStudentEntity> findAllPenRequestBatchStudentsForGivenCriteria(@Param("mincode") String mincode,
                                                                                    @Param("penRequestBatchStatusCode") String penRequestBatchStatusCode, @Param("startDate") LocalDateTime startDate, List<String> penRequestBatchStudentStatusCodes);
}
