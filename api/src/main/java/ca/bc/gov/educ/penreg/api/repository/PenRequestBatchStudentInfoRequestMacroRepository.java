package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentInfoRequestMacroEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface PenRequestBatchStudentInfoRequestMacroRepository extends CrudRepository<PenRequestBatchStudentInfoRequestMacroEntity, UUID> {
  @Override
  List<PenRequestBatchStudentInfoRequestMacroEntity> findAll();
}
