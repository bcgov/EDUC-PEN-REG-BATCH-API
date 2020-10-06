package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.*;


/**
 * The type Pen request batch student possible match entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestBatchStudentPossibleMatch {
  /**
   * The Pen request batch student possible match id.
   */
  String penRequestBatchStudentPossibleMatchId;
  /**
   * The Pen request batch student id.
   */
  String penRequestBatchStudentId;
  /**
   * The Matched student id.
   */
  String matchedStudentId;
  /**
   * The Matched priority.
   */
  Integer matchedPriority;
  /**
   * The Matched pen.
   */
  String matchedPen;
}
