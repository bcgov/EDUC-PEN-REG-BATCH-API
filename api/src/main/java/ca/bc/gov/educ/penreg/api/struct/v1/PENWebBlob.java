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
  /**
   * The Pen web blob id.
   */
  private Long penWebBlobId;
  /**
   * The Min code.
   */
  private String mincode;
  /**
   * The File name.
   */
  private String fileName;
  /**
   * The File type.
   */
  private String fileType;
  /**
   * The File contents.
   */
  private String fileContents;
  /**
   * The Insert date time.
   */
  private String insertDateTime;
  /**
   * The Extract date time.
   */
  private String extractDateTime;
  /**
   * The Submission number.
   */
  private String submissionNumber;
  /**
   * The Source application.
   */
  private String sourceApplication;
  /**
   * The Student count.
   */
  private Long studentCount;
  /**
   * The Tsw account.
   */
  private String tswAccount;
}
