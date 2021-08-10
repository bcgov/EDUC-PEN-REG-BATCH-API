package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.DUPLICATE;

@Slf4j
public abstract class ReportListItemDecorator implements ReportListItemMapper {

  /**
   * The Delegate
   */
  private final ReportListItemMapper delegate;

  protected ReportListItemDecorator(final ReportListItemMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public ReportUserMatchedListItem toReportUserMatchedListItem(final PenRequestBatchStudent penRequestBatchStudent, final Student student) {
    final var data = this.delegate.toReportUserMatchedListItem(penRequestBatchStudent, student);

    data.setMin(this.toReportListItem(student));
    data.setSchool(this.toReportListItem(penRequestBatchStudent, ""));
    return data;
  }

  @Override
  public ReportUserMatchedListItem toReportUserMatchedDiffListItem(final PenRequestBatchStudent penRequestBatchStudent, final Student student) {
    final var data = this.toReportUserMatchedListItem(penRequestBatchStudent, student);
    if (StringUtils.isNotBlank(penRequestBatchStudent.getSubmittedPen())) {
      data.getSchool().setPen(penRequestBatchStudent.getSubmittedPen());
    }

    return data;
  }

  @Override
  public ReportListItem toReportListItem(final PenRequestBatchStudent penRequestBatchStudent, final String penRequestBatchStudentValidationIssues) {
    final var studentData = this.delegate.toReportListItem(penRequestBatchStudent, penRequestBatchStudentValidationIssues);
    if (!StringUtils.isBlank(penRequestBatchStudent.getInfoRequest())) {
      studentData.setReason(penRequestBatchStudent.getInfoRequest());
    } else if (!StringUtils.isBlank(penRequestBatchStudentValidationIssues)) {
      studentData.setReason(penRequestBatchStudentValidationIssues);
    } else if (DUPLICATE.toString().equals(penRequestBatchStudent.getPenRequestBatchStudentStatusCode())) {
      studentData.setPen("Duplicate");
      studentData.setReason("This record is likely a duplicate of another record. Please confirm record is valid and resend or remove from your system.");
    }
    return this.setBirthDateAndUsualName(studentData, penRequestBatchStudent.getDob(), penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames(), "yyyyMMdd");
  }

  @Override
  public ReportListItem toReportListItem(final Student student) {
    final var studentData = this.delegate.toReportListItem(student);
    return this.setBirthDateAndUsualName(studentData, student.getDob(), student.getUsualLastName(), student.getUsualFirstName(), student.getUsualMiddleNames(), "yyyy-MM-dd");
  }

  private ReportListItem setBirthDateAndUsualName(final ReportListItem studentData, final String dob, final String usualLast, final String usualFirst, final String usualMiddle, final String pattern) {
    if (dob == null || dob.isEmpty()) {
      studentData.setBirthDate("");
    } else {
      try {
        studentData.setBirthDate(
          LocalDate.parse(dob, java.time.format.DateTimeFormatter.ofPattern(pattern))
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")));
      } catch (final DateTimeParseException e) {
        log.warn("Birth date string is un-parsable. Using raw string.");
        studentData.setBirthDate(dob);
      }
    }

    studentData.setUsualName(this.populateUsualName(usualLast, usualFirst, usualMiddle));

    return studentData;
  }
}
