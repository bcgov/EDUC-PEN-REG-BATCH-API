package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

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
}
