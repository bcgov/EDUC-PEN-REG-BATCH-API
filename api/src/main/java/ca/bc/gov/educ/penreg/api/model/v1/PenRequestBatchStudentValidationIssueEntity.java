package ca.bc.gov.educ.penreg.api.model.v1;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

/**
 * The type Pen request batch student validation issue entity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE")
public class PenRequestBatchStudentValidationIssueEntity {
  /**
   * The Pen request batch student validation issue id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchStudentValidationIssueId;

  /**
   * The Pen request batch student entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = PenRequestBatchStudentEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_STUDENT_ID", referencedColumnName = "PEN_REQUEST_BATCH_STUDENT_ID", updatable = false)
  PenRequestBatchStudentEntity penRequestBatchStudentEntity;
  /**
   * The Pen request batch validation issue severity code by pen request batch student validation issue severity code.
   */
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_SEVERITY_CODE", nullable = false)
  String penRequestBatchValidationIssueSeverityCode;
  /**
   * The Pen request batch validation issue type code by pen request batch student validation issue type code.
   */
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_TYPE_CODE", nullable = false)
  String penRequestBatchValidationIssueTypeCode;
  /**
   * The Pen request batch validation field code by pen request batch student validation field code.
   */
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_VALIDATION_FIELD_CODE", nullable = false)
  String penRequestBatchValidationFieldCode;
}
