package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.StudentEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends CrudRepository<StudentEntity, UUID>, JpaSpecificationExecutor<StudentEntity> {
  Optional<StudentEntity> findStudentEntityByPen(String pen);

  Optional<StudentEntity> findStudentEntityByEmail(String email);
  List<StudentEntity> findAll();

}
