package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestBatchStudentID {
  /**
   * The Pen request batch student id.
   */
  String penRequestBatchStudentID;
}
