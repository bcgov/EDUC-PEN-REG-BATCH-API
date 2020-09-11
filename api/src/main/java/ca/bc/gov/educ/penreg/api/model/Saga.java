package ca.bc.gov.educ.penreg.api.model;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Saga.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "PEN_REQUEST_BATCH_SAGA")
@DynamicUpdate
public class Saga {
  /**
   * The Saga id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SAGA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaId;

  /**
   * The Pen request batch student id.
   */
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_ID", columnDefinition = "BINARY(16)")
  UUID penRequestBatchStudentID;

  /**
   * The Pen request batch id.
   */
  @Column(name = "PEN_REQUEST_BATCH_ID", columnDefinition = "BINARY(16)")
  UUID penRequestBatchID;

  /**
   * The Saga name.
   */
  @NotNull(message = "saga name cannot be null")
  @Column(name = "SAGA_NAME")
  String sagaName;

  /**
   * The Saga state.
   */
  @NotNull(message = "saga state cannot be null")
  @Column(name = "SAGA_STATE")
  String sagaState;

  /**
   * The Payload.
   */
  @NotNull(message = "payload cannot be null")
  @Column(name = "PAYLOAD", length = 4000)
  String payload;

  /**
   * The Status.
   */
  @NotNull(message = "status cannot be null")
  @Column(name = "STATUS")
  String status;

  /**
   * The Create user.
   */
  @NotNull(message = "create user cannot be null")
  @Column(name = "CREATE_USER", updatable = false)
  @Size(max = 32)
  String createUser;

  /**
   * The Update user.
   */
  @NotNull(message = "update user cannot be null")
  @Column(name = "UPDATE_USER")
  @Size(max = 32)
  String updateUser;

  /**
   * The Create date.
   */
  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  /**
   * The Update date.
   */
  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

}
