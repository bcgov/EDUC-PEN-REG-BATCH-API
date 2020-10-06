package ca.bc.gov.educ.penreg.api.model;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

/**
 * The type Pen request batch student possible match entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PEN_REQUEST_BATCH_STUDENT_POSSIBLE_MATCH")
public class PenRequestBatchStudentPossibleMatchEntity {

  /**
   * The Pen request batch student possible match id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_POSSIBLE_MATCH_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchStudentPossibleMatchId;

  /**
   * The Pen request batch student entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = PenRequestBatchStudentEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_STUDENT_ID", referencedColumnName = "PEN_REQUEST_BATCH_STUDENT_ID", updatable = false)
  PenRequestBatchStudentEntity penRequestBatchStudentEntity;

  /**
   * The Matched student id.
   */
  @Basic
  @Column(name = "MATCHED_STUDENT_ID", nullable = false)
  UUID matchedStudentId;

  /**
   * The Matched priority.
   */
  @Basic
  @Column(name = "MATCHED_PRIORITY", nullable = false)
  Integer matchedPriority;

  /**
   * The Matched pen.
   */
  @Basic
  @Column(name = "MATCHED_PEN", nullable = false, length = 9)
  String matchedPen;


}
