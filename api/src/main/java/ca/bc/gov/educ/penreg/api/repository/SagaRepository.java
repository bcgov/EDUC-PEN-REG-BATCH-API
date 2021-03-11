package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends JpaRepository<Saga, UUID> {
  /**
   * Find all by status in list.
   *
   * @param statuses the statuses
   * @return the list
   */
  List<Saga> findAllByStatusIn(List<String> statuses);

  /**
   * Find all list.
   *
   * @return the list
   */
  @Override
  List<Saga> findAll();

  /**
   * Find by pen request batch student id optional.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @param sagaName                 the saga name
   * @return the optional Saga
   */
  List<Saga> findByPenRequestBatchStudentIDAndSagaName(UUID penRequestBatchStudentID, String sagaName);

  /**
   * Find by pen request batch id list.
   *
   * @param penRequestBatchID the pen request batch id
   * @param sagaName          the saga name
   * @return the list
   */
  List<Saga> findByPenRequestBatchIDAndSagaName(UUID penRequestBatchID, String sagaName);

  /**
   * Find by pen request batch student id and status
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @param sagaName                 the saga name
   * @param statuses                 the statuses
   * @return the list
   */
  List<Saga> findAllByPenRequestBatchStudentIDAndSagaNameAndStatusIn(UUID penRequestBatchStudentID, String sagaName, List<String> statuses);

  /**
   * Find by pen request batch student ids and statuses
   *
   * @param penRequestBatchStudentIDs the list pen request batch student ids
   * @param sagaName                 the saga name
   * @param statuses                 the statuses
   * @return the list
   */
  List<Saga> findAllByPenRequestBatchStudentIDInAndSagaNameAndStatusIn(List<UUID> penRequestBatchStudentIDs, String sagaName, List<String> statuses);

  List<Saga> findAllByCreateDateBefore(LocalDateTime createDate);
}
