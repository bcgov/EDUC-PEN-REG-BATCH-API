package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * The interface Pen request batch repository.
 */
@Repository
public interface PenRequestBatchHistoryRepository extends JpaRepository<PenRequestBatchHistoryEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchHistoryEntity>, PenRequestBatchHistorySearchRepository {
  List<PenRequestBatchHistoryEntity> findAllByPenRequestBatchEntity(PenRequestBatchEntity entity);
}
