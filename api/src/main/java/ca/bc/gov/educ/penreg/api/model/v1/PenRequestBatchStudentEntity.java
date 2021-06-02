package ca.bc.gov.educ.penreg.api.model.v1;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.NamedNativeQuery;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The type Pen request batch student entity.
 *
 * @author OM
 */
@Entity
@Table(name = "PEN_REQUEST_BATCH_STUDENT")
@Data
@DynamicUpdate
@NamedNativeQuery(
  name="PenRequestBatchStudentEntity.getAllPenRequestBatchStudentIDs",
  query = "SELECT PEN_REQUEST_BATCH_STUDENT_ID, PEN_REQUEST_BATCH_ID FROM PEN_REQUEST_BATCH_STUDENT WHERE PEN_REQUEST_BATCH_ID IN (?1) AND PEN_REQUEST_BATCH_STUDENT_STATUS_CODE IN (?2)",
  resultClass = PenRequestBatchStudentEntity.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchStudentEntity {

  /**
   * The Pen request batch student id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID penRequestBatchStudentID;


  /**
   * The Pen request batch entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = PenRequestBatchEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_ID", referencedColumnName = "PEN_REQUEST_BATCH_ID", updatable = false)
  PenRequestBatchEntity penRequestBatchEntity;

  /**
   * The Pen request student status code.
   */
  @Column(name = "PEN_REQUEST_BATCH_STUDENT_STATUS_CODE", nullable = false, length = 10)
  String penRequestBatchStudentStatusCode;

  /**
   * The Pen request repeat sequence number.
   */
  @Column(name = "REPEAT_REQUEST_SEQUENCE_NUMBER")

  Integer repeatRequestSequenceNumber;

  /**
   * The Pen request original repeat ID.
   */
  @Column(name = "REPEAT_REQUEST_ORIGINAL_ID", columnDefinition = "BINARY(16)")

  UUID repeatRequestOriginalID;

  /**
   * The Local id.
   */
  @Column(name = "LOCAL_ID", length = 12)
  String localID;

  /**
   * The Submitted pen.
   */
  @Column(name = "SUBMITTED_PEN", length = 9)
  String submittedPen;

  /**
   * The Legal first name.
   */
  @Column(name = "LEGAL_FIRST_NAME")
  String legalFirstName;

  /**
   * The Legal middle names.
   */
  @Column(name = "LEGAL_MIDDLE_NAMES")
  String legalMiddleNames;

  /**
   * The Legal last name.
   */
  @Column(name = "LEGAL_LAST_NAME")
  String legalLastName;

  /**
   * The Usual first name.
   */
  @Column(name = "USUAL_FIRST_NAME")
  String usualFirstName;

  /**
   * The Usual middle names.
   */
  @Column(name = "USUAL_MIDDLE_NAMES")
  String usualMiddleNames;

  /**
   * The Usual last name.
   */
  @Column(name = "USUAL_LAST_NAME")
  String usualLastName;

  /**
   * The Dob.
   */
  @Column(name = "DOB", length = 8)
  String dob;

  /**
   * The Gender code.
   */
  @Column(name = "GENDER_CODE", length = 1)
  String genderCode;

  /**
   * The Grade code.
   */
  @Column(name = "GRADE_CODE", length = 2)
  String gradeCode;


  /**
   * The Postal code.
   */
  @Column(name = "POSTAL_CODE", length = 6)
  String postalCode;

  /**
   * The Assigned pen.
   */
  @Column(name = "ASSIGNED_PEN", length = 9)
  String assignedPEN;

  /**
   * The Student id.
   */
  @Column(name = "STUDENT_ID", columnDefinition = "BINARY(16)")
  UUID studentID;
  /**
   * The Create user.
   */
  @Basic
  @Column(name = "CREATE_USER", updatable = false, nullable = false, length = 32)
  String createUser;


  /**
   * The Create date.
   */
  @Basic
  @Column(name = "CREATE_DATE", updatable = false, nullable = false)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime createDate;

  /**
   * The Update user.
   */
  @Basic
  @Column(name = "UPDATE_USER", nullable = false, length = 32)
  String updateUser;

  /**
   * The Update date.
   */
  @Basic
  @Column(name = "UPDATE_DATE", nullable = false)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime updateDate;

  /**
   * The Match algorithm status code.
   */
  @Basic
  @Column(name = "MATCH_ALGORITHM_STATUS_CODE", length = 10)
  String matchAlgorithmStatusCode;

  /**
   * The Questionable match student id.
   */
  @Basic
  @Column(name = "QUESTIONABLE_MATCH_STUDENT_ID", columnDefinition = "BINARY(16)")
  UUID questionableMatchStudentId;

  /**
   * The Info request.
   */
  @Basic
  @Column(name = "INFO_REQUEST", length = 4000)
  String infoRequest;

  /**
   * The Record number.
   */
  @Basic
  @Column(name = "RECORD_NUMBER")
  Integer recordNumber;

  /**
   * The best match pen.
   */
  @Column(name = "BEST_MATCH_PEN", length = 9)
  String bestMatchPEN;

  /**
   * The Pen request batch student validation issue entities.
   */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "penRequestBatchStudentEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = PenRequestBatchStudentValidationIssueEntity.class)
  Set<PenRequestBatchStudentValidationIssueEntity> penRequestBatchStudentValidationIssueEntities;

  /**
   * Gets pen request batch student validation issue entities.
   *
   * @return the pen request batch student validation issue entities
   */
  public Set<PenRequestBatchStudentValidationIssueEntity> getPenRequestBatchStudentValidationIssueEntities() {
    if (this.penRequestBatchStudentValidationIssueEntities == null) {
      this.penRequestBatchStudentValidationIssueEntities = new HashSet<>();
    }
    return this.penRequestBatchStudentValidationIssueEntities;
  }
}
