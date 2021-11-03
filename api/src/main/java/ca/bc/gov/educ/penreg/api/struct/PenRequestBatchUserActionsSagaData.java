package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Pen request batch student saga data.
 */
@SuperBuilder
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
