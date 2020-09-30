package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentPossibleMatchEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PenRequestBatchStudentPossibleMatchRepository extends CrudRepository<PenRequestBatchStudentPossibleMatchEntity, UUID> {
}
