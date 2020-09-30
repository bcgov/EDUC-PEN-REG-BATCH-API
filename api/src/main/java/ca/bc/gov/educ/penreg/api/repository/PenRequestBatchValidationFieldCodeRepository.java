package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchValidationFieldCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenRequestBatchValidationFieldCodeRepository extends CrudRepository<PenRequestBatchValidationFieldCodeEntity, String> {
}
