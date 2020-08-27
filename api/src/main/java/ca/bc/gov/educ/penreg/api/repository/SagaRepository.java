package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.Saga;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends CrudRepository<Saga, UUID> {
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
  List<Saga> findAll();

  /**
   * Find by pen request batch student id optional.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the optional Saga
   */
  Optional<Saga> findByPenRequestBatchStudentID(UUID penRequestBatchStudentID);

  /**
   * Find by pen request batch id list.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the list
   */
  List<Saga> findByPenRequestBatchID(UUID penRequestBatchID);
}
