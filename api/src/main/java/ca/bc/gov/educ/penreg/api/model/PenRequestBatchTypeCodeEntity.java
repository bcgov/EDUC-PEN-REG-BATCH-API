package ca.bc.gov.educ.penreg.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * The type Pen request batch type code entity.
 *  @author OM
 */
@Entity
@Table(name = "PEN_REQUEST_BATCH_TYPE_CODE")
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchTypeCodeEntity {
  /**
   * The Status code.
   */
  @Id
  @Column(name = "PEN_REQUEST_BATCH_TYPE_CODE", unique = true, length = 10)
  String statusCode;

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
