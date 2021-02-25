package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenCoordinator {
  Integer districtNumber;
  Integer schoolNumber;
  String penCoordinatorName;
  String penCoordinatorEmail;
  String penCoordinatorFax;
  String sendPenResultsVia;
}
