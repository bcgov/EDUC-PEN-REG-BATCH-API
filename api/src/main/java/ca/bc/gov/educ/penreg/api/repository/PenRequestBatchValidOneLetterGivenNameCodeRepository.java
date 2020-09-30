package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchValidOneLetterGivenNameCodeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenRequestBatchValidOneLetterGivenNameCodeRepository extends CrudRepository<PenRequestBatchValidOneLetterGivenNameCodeEntity, String> {

  List<PenRequestBatchValidOneLetterGivenNameCodeEntity> findAll();
}
