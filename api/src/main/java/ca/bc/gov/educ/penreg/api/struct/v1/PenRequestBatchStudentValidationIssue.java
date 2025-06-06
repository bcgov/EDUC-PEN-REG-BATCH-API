package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.struct.PenRequestValidationIssue;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * The type Pen request batch student validation issue.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class PenRequestBatchStudentValidationIssue extends PenRequestValidationIssue {
  String penRequestBatchStudentID;
}
