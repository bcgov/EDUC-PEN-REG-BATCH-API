package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentPossibleMatchEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The interface Pen request batch student possible match repository.
 */
@Repository
public interface PenRequestBatchStudentPossibleMatchRepository extends CrudRepository<PenRequestBatchStudentPossibleMatchEntity, UUID> {
}
