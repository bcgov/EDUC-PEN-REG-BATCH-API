package ca.bc.gov.educ.penreg.api.struct;

import lombok.*;

/**
 * The type Pen match record.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenMatchRecord {
  /**
   * The Matching pen.
   */
  private String matchingPEN;
  /**
   * The Student id.
   */
  private String studentID;
}
