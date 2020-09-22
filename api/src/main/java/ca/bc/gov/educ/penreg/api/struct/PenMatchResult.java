package ca.bc.gov.educ.penreg.api.struct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Pen match result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenMatchResult {

  /**
   * The Matching records.
   */
  private List<PenMatchRecord> matchingRecords;
  /**
   * The Pen status.
   */
  private String penStatus;
  /**
   * The Pen status message.
   */
  private String penStatusMessage;
}
