package ca.bc.gov.educ.penreg.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * The type Pen web blob entity.
 */
@Entity
@Table(name = "TSW_PENWEB_BLOBS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class PENWebBlobEntity {
  @Id
  @Column(name = "PENWEB_BLOBS_IDX", length = 18, unique = true, nullable = false, updatable = false)
  private Long penWebBlobId;

  @Column(name = "MINCODE", length = 8, nullable = false, updatable = false)
  private String minCode;

  @Column(name = "FILE_NAME", length = 64, nullable = false, updatable = false)
  private String fileName;

  @Column(name = "FILE_TYPE", length = 4, nullable = false, updatable = false)
  private String fileType;

  @Column(name = "FILE_BLOB", nullable = false, updatable = false)
  private byte[] fileContents;

  @Column(name = "INSERT_DATE_TIME")
  private LocalDateTime insertDateTime;

  @Column(name = "EXTRACT_DATE_TIME")
  private LocalDateTime extractDateTime;

  @Column(name = "SUBMISSION_NO",length = 8)
  private String submissionNumber;

  @Column(name = "SOURCE_APPLIC",length = 6)
  private String sourceApplication;

  @Column(name = "STUDENT_CNT",length = 7)
  private Long studentCount;

  @Column(name = "TSW_ACCOUNT",length = 8)
  private String tswAccount;


}
