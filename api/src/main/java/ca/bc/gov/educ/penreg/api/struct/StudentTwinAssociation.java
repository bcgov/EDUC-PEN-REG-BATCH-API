package ca.bc.gov.educ.penreg.api.struct;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentTwinAssociation implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotNull(message = "Twin Student ID can not be null.")
  String twinStudentID;
  @NotNull(message = "Student Twin Reason Code can not be null.")
  String studentTwinReasonCode;
}
