package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

/**
 * The type Saga event state.
 *
 * @param <T> the type parameter
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class SagaEventState<T> {
  /**
   * The Current event outcome.
   */
  EventOutcome currentEventOutcome;
  /**
   * The function to check the next step
   */
  Predicate<T> nextStepPredicate;
  /**
   * The Next event type.
   */
  EventType nextEventType;
  /**
   * The Step to execute.
   */
  SagaStep<T> stepToExecute;
}
