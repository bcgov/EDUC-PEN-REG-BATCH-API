package ca.bc.gov.educ.penreg.api.struct.v1;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SagaEvent {

  private UUID sagaEventId;
  private UUID sagaId;
  private String sagaEventState;
  private String sagaEventOutcome;
  private Integer sagaStepNumber;
  private String sagaEventResponse;
  private String createUser;
  private String updateUser;
  private String createDate;
  private String updateDate;
}
