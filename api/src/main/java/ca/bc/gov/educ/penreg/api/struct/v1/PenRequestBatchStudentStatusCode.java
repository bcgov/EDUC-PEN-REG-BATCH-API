package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The type Pen request batch student status code entity.
 *
 * @author OM
 */
@Data
@SuppressWarnings("squid:S1700")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchStudentStatusCode {

  /**
   * The Pen request batch student status code.
   */
  String penRequestBatchStudentStatusCode;

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
