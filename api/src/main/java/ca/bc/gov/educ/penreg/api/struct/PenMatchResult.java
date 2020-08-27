package ca.bc.gov.educ.penreg.api.struct;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.PriorityQueue;

/**
 * The type Pen match result.
 */
@Data
@AllArgsConstructor
public class PenMatchResult {

  /**
   * The Matching records.
   */
  private PriorityQueue<PenMatchRecord> matchingRecords;
  /**
   * The Pen.
   */
  private String pen;
  /**
   * The Pen status.
   */
  private String penStatus;
  /**
   * The Pen status message.
   */
  private String penStatusMessage;
}
