package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PenRequestBatchRepository extends CrudRepository<PenRequestBatchEntity, UUID>, JpaSpecificationExecutor<PenRequestBatchEntity> {
}
