package ca.bc.gov.educ.penreg.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * The type Pen request batch validation issue severity code entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE")
public class PenRequestBatchValidationIssueSeverityCodeEntity {
  /**
   * The Code.
   */
  @Id
  @Column(name = "CODE", nullable = false, length = 20)
  private String code;
  /**
   * The Label.
   */
  @Basic
  @Column(name = "LABEL", nullable = false, length = 30)
  private String label;
  /**
   * The Description.
   */
  @Basic
  @Column(name = "DESCRIPTION")
  private String description;
  /**
   * The Display order.
   */
  @Basic
  @Column(name = "DISPLAY_ORDER")
  private Integer displayOrder;

  /**
   * The Effective date.
   */
  @Basic
  @Column(name = "EFFECTIVE_DATE", nullable = false)
  private LocalDateTime effectiveDate;
  /**
   * The Expiry date.
   */
  @Basic
  @Column(name = "EXPIRY_DATE", nullable = false)
  private LocalDateTime expiryDate;
  /**
   * The Create user.
   */
  @Basic
  @Column(name = "CREATE_USER", nullable = false, length = 32)
  private String createUser;
  /**
   * The Create date.
   */
  @Basic
  @Column(name = "CREATE_DATE", nullable = false)
  private LocalDateTime createDate;

  /**
   * The Update user.
   */
  @Basic
  @Column(name = "UPDATE_USER", nullable = false, length = 32)
  private String updateUser;

  /**
   * The Update date.
   */
  @Basic
  @Column(name = "UPDATE_DATE", nullable = false)
  private LocalDateTime updateDate;

}
