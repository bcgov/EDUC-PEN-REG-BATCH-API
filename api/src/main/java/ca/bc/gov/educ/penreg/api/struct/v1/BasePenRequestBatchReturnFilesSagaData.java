package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.struct.*;
import java.util.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class BasePenRequestBatchReturnFilesSagaData extends BaseRequest {

    /**
     * The pen request batch id.
     */
    @NotNull(message = "penRequestBatchID cannot be null")
    UUID penRequestBatchID;
    @NotNull( message = "schoolName cannot be null")
    String schoolName;

    /**
     *
     * The below are added during the saga process, not the initial call
     */
    PenRequestBatch penRequestBatch;
    List<PenRequestBatchStudent> penRequestBatchStudents;

  @Setter(AccessLevel.NONE)
    List<Student> students;

  public void setStudents(Event event, List<Student> students) {
    if (students == null) {
      log.info("changing students to null in BasePenRequestBatchReturnFilesSagaData for saga id :: {} and event type:: {} and event outcome :: {}",event.getSagaId(), event.getEventType(), event.getEventOutcome());
    }
    this.students = students;
  }
    Map<String, String> penRequestBatchStudentValidationIssues;

    List<SchoolContact> studentRegistrationContacts;
  public List<SchoolContact> getStudentRegistrationContacts() {
    if (this.studentRegistrationContacts == null) {
      return new ArrayList<>();
    }
    return this.studentRegistrationContacts;
  }

    String fromEmail;
    String telephone;
    String facsimile;
    String mailingAddress;
}
