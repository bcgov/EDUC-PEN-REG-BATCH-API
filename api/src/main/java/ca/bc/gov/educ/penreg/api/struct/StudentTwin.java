package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentTwin implements Serializable {
  private static final long serialVersionUID = 1L;

  String studentID;
  String twinStudentID;
  String studentTwinReasonCode;
  String createUser;
  String updateUser;

}
