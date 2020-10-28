package ca.bc.gov.educ.penreg.api.struct;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * The type Pen request batch student saga data.
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestBatchStudentSagaData extends BasePenRequestBatchStudentSagaData {
  /**
   * The Pen match result.
   */
  PenMatchResult penMatchResult;

  /**
   * The Generate pen.
   * this is important in replay scenario when pod dies, rather than generating a new PEN use the PEN it was already generated.
   */
  String generatedPEN;

  /**
   * The Is pen match results processed.
   * this is important in replay scenario when pod dies
   */
  Boolean isPENMatchResultsProcessed;
}
