package ca.bc.gov.educ.penreg.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "STUDENT")
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID studentID;
  @NotNull(message = "pen cannot be null")
  @Column(name = "PEN")
  String pen;
  @Column(name = "LEGAL_FIRST_NAME")
  String legalFirstName;
  @Column(name = "LEGAL_MIDDLE_NAMES")
  String legalMiddleNames;
  @Column(name = "LEGAL_LAST_NAME")
  String legalLastName;
  @Column(name = "DOB")
  @PastOrPresent
  LocalDate dob;
  @Column(name = "SEX_CODE")
  String sexCode;
  @Column(name = "GENDER_CODE")
  String genderCode;
  @Column(name = "USUAL_FIRST_NAME")
  String usualFirstName;
  @Column(name = "USUAL_MIDDLE_NAMES")
  String usualMiddleNames;
  @Column(name = "USUAL_LAST_NAME")
  String usualLastName;
  @Email(message = "Email must be valid email address")
  @Column(name = "EMAIL")
  String email;
  @NotNull(message = "Email verified cannot be null")
  @Column(name = "EMAIL_VERIFIED")
  String emailVerified;
  @Column(name = "DECEASED_DATE")
  @PastOrPresent
  LocalDate deceasedDate;
  @Column(name = "POSTAL_CODE")
  String postalCode;
  @Column(name = "MINCODE")
  String mincode;
  @Column(name = "LOCAL_ID")
  String localID;
  @Column(name = "GRADE_CODE")
  String gradeCode;
  @Column(name = "MEMO")
  String memo;
  @Column(name = "GRADE_YEAR")
  String gradeYear;
  @Column(name = "DEMOG_CODE")
  String demogCode;
  @Column(name = "STATUS_CODE")
  String statusCode;
  @Column(name = "CREATE_USER", updatable = false)
  String createUser;
  @Column(name = "CREATE_DATE", updatable = false)
  @PastOrPresent
  LocalDateTime createDate;
  @Column(name = "UPDATE_USER")
  String updateUser;
  @Column(name = "UPDATE_DATE")
  @PastOrPresent
  LocalDateTime updateDate;

}
