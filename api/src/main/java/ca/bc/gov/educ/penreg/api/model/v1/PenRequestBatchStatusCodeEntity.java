package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * The type Pen request batch status code entity.
 *
 * @author OM
 */
@Entity
@Table(name = "PEN_REQUEST_BATCH_STATUS_CODE")
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchStatusCodeEntity {
  /**
   * The Status code.
   */
  @Id
  @Column(name = "PEN_REQUEST_BATCH_STATUS_CODE", unique = true, length = 10)
  String penRequestBatchStatusCode;

  /**
   * The Label.
   */
  @Column(name = "LABEL", length = 30)
  String label;

  /**
   * The Description.
   */
  @Column(name = "DESCRIPTION")
  String description;

  /**
   * The Display order.
   */
  @Column(name = "DISPLAY_ORDER")
  Integer displayOrder;

  /**
   * The Effective date.
   */
  @Column(name = "EFFECTIVE_DATE")
  LocalDateTime effectiveDate;

  /**
   * The Expiry date.
   */
  @Column(name = "EXPIRY_DATE")
  LocalDateTime expiryDate;

  /**
   * The Create user.
   */
  @Column(name = "CREATE_USER", updatable = false , length = 32)
  String createUser;

  /**
   * The Create date.
   */
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  /**
   * The Update user.
   */
  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  /**
   * The Update date.
   */
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;
}
