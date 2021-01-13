package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Possible match.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PossibleMatch {
  /**
   * The Possible match id.
   */
  String possibleMatchID;
  /**
   * The Student id.
   */
  String studentID;
  /**
   * The Matched student id.
   */
  String matchedStudentID;
  /**
   * The Match reason code.
   */
  String matchReasonCode;
  /**
   * The Create user.
   */
  String createUser;
  /**
   * The Update user.
   */
  String updateUser;


}