package ca.bc.gov.educ.penreg.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.util.UUID;

/**
 * The type Pen request batch student entity.
 *  @author OM
 */
@Entity
@Table(name = "PEN_REQUEST_BATCH_STUDENT")
@Data
@DynamicUpdate
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
  @ManyToOne(cascade = CascadeType.ALL, optional = false, targetEntity = PenRequestBatchEntity.class)
  @JoinColumn(name = "PEN_REQUEST_BATCH_ID", referencedColumnName = "PEN_REQUEST_BATCH_ID", updatable = false, insertable = false)
  PenRequestBatchEntity penRequestBatchEntity;

  /**
   * The Pen request student status code.
   */
  @Column(name="PEN_REQUEST_STUDENT_STATUS_CODE", length = 10)
  String penRequestStudentStatusCode;

  /**
   * The Local id.
   */
  @Column(name="LOCAL_ID", length = 12)
  String localID;

  /**
   * The Submitted pen.
   */
  @Column(name="SUBMITTED_PEN", length = 9)
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
  String studentID;

}