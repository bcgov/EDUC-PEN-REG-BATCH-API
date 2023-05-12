package ca.bc.gov.educ.penreg.api.struct;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Pen match result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
