package ca.bc.gov.educ.penreg.api.batch.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * The type Trax student web.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TraxStudentWeb {
  @NotNull
  private String fileName;
  @NotNull
  private String fileType;
  @NotNull
  @Max(8)
  private String submissionNumber;
  @NotNull
  private LocalDateTime insertDate;
  @NotNull
  private LocalDateTime extractDate;
  @NotNull
  private String tswAccount;
  @NotNull
  private String ministryPRBSourceCode;
  @NotNull
  private String sourceApplication;
  @NotNull
  private byte[] fileContents; // this contents the blob from tsw which will be processed.
}
