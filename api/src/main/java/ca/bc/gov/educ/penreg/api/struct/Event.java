package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private EventType eventType;
  private EventOutcome eventOutcome;
  private String replyTo;
  private String eventPayload;
  private UUID sagaId;

}
