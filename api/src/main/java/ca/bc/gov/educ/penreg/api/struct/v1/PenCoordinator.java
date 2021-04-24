package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenCoordinator {
  Integer districtNumber;
  Integer schoolNumber;
  String mincode;
  String penCoordinatorName;
  String penCoordinatorEmail;
  String penCoordinatorFax;
  String sendPenResultsVia;
}
