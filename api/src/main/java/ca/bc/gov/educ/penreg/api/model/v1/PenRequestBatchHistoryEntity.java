package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Pen request batch history entity.
 */
@Entity
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "PEN_REQUEST_BATCH_HISTORY")
public class PenRequestBatchHistoryEntity {

  /**
   * The Pen request batch history id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchHistoryId;

  /**
   * The Pen request batch entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = PenRequestBatchEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_ID", referencedColumnName = "PEN_REQUEST_BATCH_ID", updatable = false)
  PenRequestBatchEntity penRequestBatchEntity;

  /**
   * The Event date.
   */
  @Basic
  @Column(name = "EVENT_DATE", nullable = false)
  LocalDateTime eventDate;

  /**
   * The Pen request batch event code.
   */
  @Basic
  @Column(name = "PEN_REQUEST_BATCH_EVENT_CODE", nullable = false, length = 10)
  String penRequestBatchEventCode;

  /**
   * The Submission number.
   * this data comes from TSW table
   */
  @Column(name = "SUBMISSION_NO", length = 8, nullable = false)
  String submissionNumber;

  /**
   * The Pen request batch status code.
   */
  @Column(name = "PEN_REQUEST_BATCH_STATUS_CODE", length = 10, nullable = false)
  String penRequestBatchStatusCode;

  /**
   * The Pen request batch status reason.
   */
  @Column(name = "PEN_REQUEST_BATCH_STATUS_REASON")
  String penRequestBatchStatusReason;

  /**
   * The Pen request batch type code.
   */
  @Column(name = "PEN_REQUEST_BATCH_TYPE_CODE", nullable = false, length = 10)
  String penRequestBatchTypeCode;

  /**
   * The Ministry prb source code.
   */
  @Column(name = "MINISTRY_PRB_SOURCE_CODE", nullable = false, length = 10)
  String ministryPRBSourceCode;

  /**
   * The School group code.
   */
  @Column(name = "SCHOOL_GROUP_CODE", length = 10)
  String schoolGroupCode;

  /**
   * The File name.
   */
  @Column(name = "FILE_NAME", nullable = false)
  String fileName;

  /**
   * The File type.
   */
  @Column(name = "FILE_TYPE", length = 4, nullable = false)
  String fileType;

  /**
   * The Insert date.
   */
  @Column(name = "INSERT_DATE", nullable = false)
  LocalDateTime insertDate;

  /**
   * The Extract date.
   */
  @Column(name = "EXTRACT_DATE", nullable = false)
  LocalDateTime extractDate;

  /**
   * The Process date.
   */
  @Column(name = "PROCESS_DATE")
  LocalDateTime processDate;

  /**
   * The Source application.
   */
  @Column(name = "SOURCE_APPLICATION", length = 6, nullable = false)
  String sourceApplication;

  /**
   * The Min code.
   */
  @Column(name = "MINCODE", length = 8)
  String mincode;

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
  Long officeNumber;

  /**
   * The Source student count.
   */
  @Column(name = "SOURCE_STUDENT_COUNT")
  Long sourceStudentCount;

  /**
   * The Student count.
   */
  @Column(name = "STUDENT_COUNT")
  Long studentCount;

  /**
   * The new pen count.
   */
  @Column(name = "NEW_PEN_COUNT")
  Long newPenCount;
  /**
   * The Error count.
   */
  @Column(name = "ERROR_COUNT")
  Long errorCount;
  /**
   * The Matched count.
   */
  @Column(name = "MATCHED_COUNT")
  Long matchedCount;
  /**
   * The Repeat count.
   */
  @Column(name = "REPEAT_COUNT")
  Long repeatCount;
  /**
   * The Fixable count.
   */
  @Column(name = "FIXABLE_COUNT")
  Long fixableCount;

  @Column(name = "DUPLICATE_COUNT")
  Long duplicateCount;

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
   * The Create user.
   */
  @Basic
  @Column(name = "CREATE_USER",updatable = false, nullable = false, length = 100)
  String createUser;


  /**
   * The Create date.
   */
  @Basic
  @Column(name = "CREATE_DATE",updatable = false, nullable = false)
  LocalDateTime createDate;

  /**
   * The Update user.
   */
  @Basic
  @Column(name = "UPDATE_USER", nullable = false, length = 100)
  String updateUser;

  /**
   * The Update date.
   */
  @Basic
  @Column(name = "UPDATE_DATE", nullable = false)
  LocalDateTime updateDate;

  /**
   * The Pen request batch process type code.
   */
  @Column(name = "PEN_REQUEST_BATCH_PROCESS_TYPE_CODE",updatable = false, nullable = false, length = 10)
  String penRequestBatchProcessTypeCode;

}
