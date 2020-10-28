package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenRequestBatchEventRepository extends CrudRepository<PenRequestBatchEvent, UUID> {
  Optional<PenRequestBatchEvent> findBySagaId(UUID sagaId);

  Optional<PenRequestBatchEvent> findBySagaIdAndEventType(UUID sagaId, String eventType);

  List<PenRequestBatchEvent> findByEventStatus(String eventStatus);
}
