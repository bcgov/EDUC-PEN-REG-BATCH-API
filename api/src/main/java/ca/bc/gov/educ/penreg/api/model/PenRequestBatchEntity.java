package ca.bc.gov.educ.penreg.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The type Pen request batch entity.
 *
 * @author OM
 */
@Entity
@Table(name = "PEN_REQUEST_BATCH")
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchEntity {
  /**
   * The Pen request batch id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchID;

  /**
   * The Submission number.
   */
  @Column(name = "SUBMISSION_NO", unique = true, length = 8)
  String submissionNumber;

  /**
   * The Pen request batch status code.
   */
  @Column(name = "PENR_EQUEST_BATCH_STATUS_CODE", length = 10)
  String penRequestBatchStatusCode;

  /**
   * The Pen request batch status reason.
   */
  @Column(name = "PENR_EQUEST_BATCH_STATUS_REASON")
  String penRequestBatchStatusReason;

  /**
   * The Pen request batch type code.
   */
  @Column(name = "PEN_REQUEST_BATCH_TYPE_CODE")
  String penRequestBatchTypeCode;

  /**
   * The Unarchived flag.
   */
  @Column(name = "UNARCHIVED_FLAG", length = 1)
  String unarchivedFlag;

  /**
   * The Unarchived batch changed flag.
   */
  @Column(name = "UNARCHIVED_BATCH_CHANGED_FLAG", length = 1)
  String unarchivedBatchChangedFlag;

  /**
   * The File name.
   */
  @Column(name = "FILE_NAME")
  String fileName;

  /**
   * The File type.
   */
  @Column(name = "FILE_TYPE", length = 4)
  String fileType;

  /**
   * The Insert date.
   */
  @Column(name = "INSERT_DATE")
  LocalDateTime insertDate;

  /**
   * The Extract date.
   */
  @Column(name = "EXTRACT_DATE")
  LocalDateTime extractDate;

  /**
   * The Process date.
   */
  @Column(name = "PROCESS_DATE")
  LocalDateTime processDate;

  /**
   * The Source application.
   */
  @Column(name = "SOURCE_APPLICATION", length = 6)
  String sourceApplication;

  /**
   * The Pen request batch source code.
   */
  @Column(name = "PEN_REQUEST_BATCH_SOURCE_CODE", length = 10)
  String penRequestBatchSourceCode;

  /**
   * The Tsw account.
   */
  @Column(name = "TSW_ACCOUNT", length = 8)
  String tswAccount;

  /**
   * The Min code.
   */
  @Column(name = "MINCODE", length = 8)
  String minCode;

  /**
   * The School name.
   */
  @Column(name = "SCHOOL_NAME")
  String schoolName;

  /**
   * The Contact name.
   */
  @Column(name = "CONTACT_NAME")
  String contactName;

  /**
   * The Email.
   */
  @Column(name = "EMAIL")
  String email;

  /**
   * The Office number.
   */
  @Column(name = "OFFICE_NUMBER")
  BigDecimal officeNumber;

  /**
   * The Source student count.
   */
  @Column(name = "SOURCE_STUDENT_COUNT")
  BigDecimal sourceStudentCount;

  /**
   * The Student count.
   */
  @Column(name = "STUDENT_COUNT")
  BigDecimal studentCount;

  /**
   * The Issued pen count.
   */
  @Column(name = "ISSUED_PEN_COUNT")
  BigDecimal issuedPenCount;
  /**
   * The Error count.
   */
  @Column(name = "ERROR_COUNT")
  BigDecimal errorCount;
  /**
   * The Matched count.
   */
  @Column(name = "MATCHED_COUNT")
  BigDecimal matchedCount;
  /**
   * The Repeat count.
   */
  @Column(name = "REPEAT_COUNT")
  BigDecimal repeatCount;
  /**
   * The Fixable count.
   */
  @Column(name = "FIXABLE_COUNT")
  BigDecimal fixableCount;
  /**
   * The Sis vendor name.
   */
  @Column(name = "SIS_VENDOR_NAME", length = 100)
  String sisVendorName;
  /**
   * The Sis product name.
   */
  @Column(name = "SIS_PRODUCT_NAME", length = 100)
  String sisProductName;
  /**
   * The Sis product id.
   */
  @Column(name = "SIS_PRODUCT_ID", length = 15)
  String sisProductID;
  /**
   * The Pen request batch student entities.
   */
  @OneToMany(mappedBy = "penRequestBatchEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = PenRequestBatchStudentEntity.class)
  Set<PenRequestBatchStudentEntity> penRequestBatchStudentEntities;

  public Set<PenRequestBatchStudentEntity> getPenRequestBatchStudentEntities() {
    if (this.penRequestBatchStudentEntities == null) {
      this.penRequestBatchStudentEntities = new HashSet<>();
    }
    return this.penRequestBatchStudentEntities;
  }
}