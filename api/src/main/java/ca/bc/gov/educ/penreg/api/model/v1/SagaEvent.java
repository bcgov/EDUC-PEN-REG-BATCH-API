package ca.bc.gov.educ.penreg.api.model.v1;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Saga event.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "PEN_REQUEST_BATCH_SAGA_EVENT_STATES")
@DynamicUpdate
public class SagaEvent {

  /**
   * The Saga event id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SAGA_EVENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaEventId;


  /**
   * The Saga.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne
  @JoinColumn(name = "SAGA_ID", updatable = false, columnDefinition = "BINARY(16)")
  Saga saga;

  /**
   * The Saga event state.
   */
  @NotNull(message = "saga_event_state cannot be null")
  @Column(name = "SAGA_EVENT_STATE")
  String sagaEventState;

  /**
   * The Saga event outcome.
   */
  @NotNull(message = "saga_event_outcome cannot be null")
  @Column(name = "SAGA_EVENT_OUTCOME")
  String sagaEventOutcome;

  /**
   * The Saga step number.
   */
  @NotNull(message = "saga_step_number cannot be null")
  @Column(name = "SAGA_STEP_NUMBER")
  Integer sagaStepNumber;

  /**
   * The Saga event response.
   */
  @Lob
  @Column(name = "SAGA_EVENT_RESPONSE")
  byte[] sagaEventResponseBytes;

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

  public String getSagaEventResponse() {
    return new String(this.getSagaEventResponseBytes(), StandardCharsets.UTF_8);
  }

  public void setSagaEventResponse(final String sagaEventResponse) {
    this.setSagaEventResponseBytes(sagaEventResponse.getBytes(StandardCharsets.UTF_8));
  }

  public static class SagaEventBuilder {
    byte[] sagaEventResponseBytes;

    public SagaEvent.SagaEventBuilder sagaEventResponse(final String sagaEventResponse) {
      this.sagaEventResponseBytes = sagaEventResponse.getBytes(StandardCharsets.UTF_8);
      return this;
    }
  }
}
