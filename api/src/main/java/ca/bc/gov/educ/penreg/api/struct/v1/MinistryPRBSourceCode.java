package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The type Pen request batch source code entity.
 *
 * @author OM
 */
@Data
@SuppressWarnings("squid:S1700")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinistryPRBSourceCode {
  /**
   * The Ministry prb source code.
   */
  String ministryPRBSourceCode;

  /**
   * The Label.
   */
  String label;

  /**
   * The Description.
   */
  String description;

  /**
   * The Display order.
   */
  Integer displayOrder;

  /**
   * The Effective date.
   */
  String effectiveDate;

  /**
   * The Expiry date.
   */
  String expiryDate;

}
