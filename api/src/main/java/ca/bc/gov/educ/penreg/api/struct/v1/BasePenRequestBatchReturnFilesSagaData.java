package ca.bc.gov.educ.penreg.api.struct.v1;

import ca.bc.gov.educ.penreg.api.struct.Student;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
    List<Student> students;
    Map<String, String> penRequestBatchStudentValidationIssues;
    PenCoordinator penCoordinator;

    String fromEmail;
    String telephone;
    String facsimile;
    String mailingAddress;
}
