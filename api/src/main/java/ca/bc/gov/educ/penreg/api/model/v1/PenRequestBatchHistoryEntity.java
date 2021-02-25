package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Pen request batch history entity.
 */
@Entity
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "PEN_REQUEST_BATCH_HISTORY")
public class PenRequestBatchHistoryEntity {

  /**
   * The Pen request batch history id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchHistoryId;

  /**
   * The Pen request batch entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = PenRequestBatchEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_ID", referencedColumnName = "PEN_REQUEST_BATCH_ID", updatable = false)
  PenRequestBatchEntity penRequestBatchEntity;

  /**
   * The Event date.
   */
  @Basic
  @Column(name = "EVENT_DATE", nullable = false)
  LocalDateTime eventDate;

  /**
   * The Pen request batch status code.
   */
  @Basic
  @Column(name = "PEN_REQUEST_BATCH_STATUS_CODE", nullable = false, length = 10)
  String penRequestBatchStatusCode;

  /**
   * The Pen request batch event code.
   */
  @Basic
  @Column(name = "PEN_REQUEST_BATCH_EVENT_CODE", nullable = false, length = 10)
  String penRequestBatchEventCode;

  /**
   * The Event reason.
   */
  @Basic
  @Column(name = "EVENT_REASON")
  String eventReason;

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
