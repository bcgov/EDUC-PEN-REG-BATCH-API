package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.Mincode;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenCoordinatorRepository extends JpaRepository<PenCoordinator, Mincode> {
}
