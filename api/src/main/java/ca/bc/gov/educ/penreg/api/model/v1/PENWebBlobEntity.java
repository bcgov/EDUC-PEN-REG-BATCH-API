package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
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
  /**
   * The Pen web blob id.
   */
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "PENWEB_BLOBS_IDX", length = 18, unique = true, nullable = false, updatable = false)
  private Long penWebBlobId;

  /**
   * The Min code.
   */
  @Column(name = "MINCODE", length = 8, nullable = false, updatable = false)
  private String mincode;

  /**
   * The File name.
   */
  @Column(name = "FILE_NAME", length = 64, nullable = false, updatable = false)
  private String fileName;

  /**
   * The File type.
   */
  @Column(name = "FILE_TYPE", length = 4, nullable = false, updatable = false)
  private String fileType;

  /**
   * The File contents.
   */
// here length is provided to make sure uni tests with H2 work.
  @Column(name = "FILE_BLOB", nullable = false, updatable = false, length = 4000000)
  private byte[] fileContents;

  /**
   * The Insert date time.
   */
  @Column(name = "INSERT_DATE_TIME")
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime insertDateTime;

  /**
   * The Extract date time.
   */
  @Column(name = "EXTRACT_DATE_TIME")
  private LocalDateTime extractDateTime;

  /**
   * The Submission number.
   */
  @Column(name = "SUBMISSION_NO",length = 8)
  private String submissionNumber;

  /**
   * The Source application.
   */
  @Column(name = "SOURCE_APPLIC",length = 6)
  private String sourceApplication;

  /**
   * The Student count.
   */
  @Column(name = "STUDENT_CNT",length = 7)
  private Long studentCount;

  /**
   * The Tsw account.
   */
  @Column(name = "TSW_ACCOUNT",length = 8)
  private String tswAccount;


}
