package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * The interface Pen request batch repository.
 */
@Repository
public interface PenRequestBatchHistoryRepository extends JpaRepository<PenRequestBatchHistoryEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchHistoryEntity>, PenRequestBatchHistorySearchRepository {
  List<PenRequestBatchHistoryEntity> findAllByPenRequestBatchEntity(PenRequestBatchEntity entity);
}
