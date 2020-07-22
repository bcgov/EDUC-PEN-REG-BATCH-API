package ca.bc.gov.educ.penreg.api.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S1700")
public class StatusCode implements Serializable {
  String statusCode;
  String label;
  String description;
  Integer displayOrder;
  String effectiveDate;
  String expiryDate;
}
