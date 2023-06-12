package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * The type Pen request batch event code entity.
 */
@Entity
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "PEN_REQUEST_BATCH_EVENT_CODE")
public class PenRequestBatchEventCodeEntity {

  /**
   * The Pen request batch event code.
   */
  @Id
  @Column(name = "PEN_REQUEST_BATCH_EVENT_CODE", nullable = false, length = 10)
  String penRequestBatchEventCode;

  /**
   * The Label.
   */
  @Basic
  @Column(name = "LABEL", length = 30)
  String label;

  /**
   * The Description.
   */
  @Basic
  @Column(name = "DESCRIPTION")
  String description;

  /**
   * The Display order.
   */
  @Basic
  @Column(name = "DISPLAY_ORDER", nullable = false)
  Long displayOrder;

  /**
   * The Effective date.
   */
  @Basic
  @Column(name = "EFFECTIVE_DATE", nullable = false)
  LocalDateTime effectiveDate;

  /**
   * The Expiry date.
   */
  @Basic
  @Column(name = "EXPIRY_DATE", nullable = false)
  LocalDateTime expiryDate;

  /**
   * The Create user.
   */
  @Basic
  @Column(name = "CREATE_USER", nullable = false, length = 32)
  String createUser;

  /**
   * The Create date.
   */
  @Basic
  @Column(name = "CREATE_DATE", nullable = false)
  LocalDateTime createDate;

  /**
   * The Update user.
   */
  @Basic
  @Column(name = "UPDATE_USER", nullable = false, length = 32)
  String updateUser;

  /**
   * The Update date.
   */
  @Basic
  @Column(name = "UPDATE_DATE", nullable = false)
  LocalDateTime updateDate;

}
