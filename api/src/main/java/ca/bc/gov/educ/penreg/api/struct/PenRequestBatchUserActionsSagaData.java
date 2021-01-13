package ca.bc.gov.educ.penreg.api.struct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
   * The Matched student id list.
   */
  List<String> matchedStudentIDList;

  /**
   * The record number.
   */
  Integer recordNumber;
}
