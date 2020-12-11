package ca.bc.gov.educ.penreg.api.struct;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Pen request batch student saga data.
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PenRequestBatchUserActionsSagaData extends BasePenRequestBatchStudentSagaData {
  /**
   * The Twins
   */
  List<String> twinStudentIDs;

  /**
   * The record number.
   */
  Integer recordNumber;
}
