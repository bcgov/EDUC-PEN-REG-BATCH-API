package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.StudentEvent;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEventRepository extends CrudRepository<StudentEvent, UUID> {
  Optional<StudentEvent> findBySagaId(UUID sagaId);

  Optional<StudentEvent> findBySagaIdAndEventType(UUID sagaId, String eventType);

  List<StudentEvent> findByEventStatus(String eventStatus);
}
