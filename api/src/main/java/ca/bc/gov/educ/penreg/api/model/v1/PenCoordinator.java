package ca.bc.gov.educ.penreg.api.model.v1;

import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "PEN_COORDINATOR")
@Data
@Immutable
public class PenCoordinator {

  @EmbeddedId
  Mincode mincode;

  @Column(name = "PEN_COORDINATOR_NAME")
  String penCoordinatorName;
  @Column(name = "PEN_COORDINATOR_EMAIL")
  String penCoordinatorEmail;
  @Column(name = "PEN_COORDINATOR_FAX")
  String penCoordinatorFax;
  @Column(name = "SEND_PEN_RESULTS_VIA")
  String sendPenResultsVia;
}
