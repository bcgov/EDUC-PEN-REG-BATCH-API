package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * The type Pen request batch student issue type code entity.
 *
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchStudentValidationIssueFieldCode {
  /**
   * The Pen request batch student issue field code.
   */
  String code;

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
