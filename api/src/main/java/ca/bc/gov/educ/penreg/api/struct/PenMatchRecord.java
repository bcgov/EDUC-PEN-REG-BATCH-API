package ca.bc.gov.educ.penreg.api.struct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Pen match record.
 */
@Getter
@Setter
@AllArgsConstructor
public class PenMatchRecord {
  /**
   * The Matching algorithm result.
   */
  private Integer matchingAlgorithmResult;
  /**
   * The Matching score.
   */
  private Integer matchingScore;
  /**
   * The Matching pen.
   */
  private String matchingPEN;
  /**
   * The Student id.
   */
  private String studentID;
}
