package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Pen web blob entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PENWebBlob {
  private Long penWebBlobId;
  private String minCode;
  private String fileName;
  private String fileType;
  private String insertDateTime;
  private String extractDateTime;
  private String submissionNumber;
  private String sourceApplication;
  private Long studentCount;
  private String tswAccount;
}
