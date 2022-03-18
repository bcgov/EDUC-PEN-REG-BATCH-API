package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenRequestBatchEventRepository extends CrudRepository<PenRequestBatchEvent, UUID> {
  Optional<PenRequestBatchEvent> findBySagaId(UUID sagaId);

  Optional<PenRequestBatchEvent> findBySagaIdAndEventType(UUID sagaId, String eventType);

  List<PenRequestBatchEvent> findByEventStatus(String eventStatus);

  @Transactional
  @Modifying
  @Query("delete from PenRequestBatchEvent where createDate <= :createDate")
  void deleteByCreateDateBefore(LocalDateTime createDate);
}
