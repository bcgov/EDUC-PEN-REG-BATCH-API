package ca.bc.gov.educ.penreg.api.struct;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Event.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  /**
   * The Event type.
   */
  private EventType eventType;
  /**
   * The Event outcome.
   */
  private EventOutcome eventOutcome;
  /**
   * The Saga id.
   */
  private UUID sagaId;
  /**
   * The Reply to.
   */
  private String replyTo;
  /**
   * The Event payload.
   */
  private String eventPayload; // json string
}
