package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.StudentDemogCode;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.BasePenRequestBatchReturnFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class PenRequestBatchReportDataDecorator implements PenRequestBatchReportDataMapper {

  private static final ReportListItemMapper listItemMapper = ReportListItemMapper.mapper;
  /**
   * The Delegate
   */
  private final PenRequestBatchReportDataMapper delegate;

  protected PenRequestBatchReportDataDecorator(final PenRequestBatchReportDataMapper mapper) {
    this.delegate = mapper;
  }

  /**
   * To pen request batch student entity pen request batch student entity.
   *
   * @param data the saga data
   * @return the pen request batch report data entity
   */
  @Override
  public PenRequestBatchReportData toReportData(final BasePenRequestBatchReturnFilesSagaData data) {
    final var reportData = this.delegate.toReportData(data);
    final List<ReportListItem> pendingList = new ArrayList<>();
    final List<ReportListItem> newPenList = new ArrayList<>();
    final List<ReportListItem> sysMatchedList = new ArrayList<>();
    final List<ReportUserMatchedListItem> diffList = new ArrayList<>();
    final List<ReportUserMatchedListItem> confirmedList = new ArrayList<>();
    final Map<String, Student> students = this.setStudents(data.getStudents());

    for (final PenRequestBatchStudent penRequestBatchStudent : data.getPenRequestBatchStudents()) {
      switch (Objects.requireNonNull(PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode()))) {
        case DUPLICATE:
        case ERROR:
        case REPEAT:
        case INFOREQ:
        case FIXABLE:
          var issues = data.getPenRequestBatchStudentValidationIssues().get(penRequestBatchStudent.getPenRequestBatchStudentID());
          pendingList.add(listItemMapper.toReportListItem(penRequestBatchStudent, issues));
          break;
        case SYS_NEW_PEN:
        case USR_NEW_PEN:
          populateForNewPenStatus(newPenList, students, penRequestBatchStudent);
          break;
        case SYS_MATCHED:
          this.populateForSystemMatchedStatus(sysMatchedList, diffList, students, penRequestBatchStudent);
          break;
        case USR_MATCHED:
          this.populateForUserMatchedStatus(diffList, confirmedList, students, penRequestBatchStudent);
          break;
        default:
          log.error("Unexpected pen request batch student error code encountered while attempting generate report data :: " + penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
          break;
      }
    }

    reportData.setSysMatchedList(sysMatchedList);
    reportData.setPendingList(pendingList);
    reportData.setNewPenList(newPenList);
    reportData.setDiffList(diffList);
    reportData.setConfirmedList(confirmedList);

    final var processDateTime = LocalDateTime.parse(data.getPenRequestBatch().getProcessDate());
    reportData.setProcessDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
    reportData.setProcessTime(processDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
    reportData.setReportDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd")).toUpperCase().replace(".", ""));

    reportData.setReviewer(this.setReviewer(data.getPenCoordinator()));

    return reportData;
  }

  private void populateForNewPenStatus(List<ReportListItem> newPenList, Map<String, Student> students, PenRequestBatchStudent penRequestBatchStudent) {
    if (students.get(penRequestBatchStudent.getStudentID()) == null) {
      log.error("Error attempting to create report data. Students list should not be null for USR_NEW_PEN status.");
      return;
    }
    newPenList.add(listItemMapper.toReportListItem(students.get(penRequestBatchStudent.getStudentID())));
  }

  private void populateForUserMatchedStatus(final List<ReportUserMatchedListItem> diffList, final List<ReportUserMatchedListItem> confirmedList, final Map<String, Student> students, final PenRequestBatchStudent penRequestBatchStudent) {
    if (students.get(penRequestBatchStudent.getStudentID()) == null) {
      log.error("Error attempting to create report data. Students list should not be null for USR_MATCHED status.");
      return;
    }
    val matchedStudent = students.get(penRequestBatchStudent.getStudentID());
    if (matchedStudent != null && matchedStudent.getDemogCode() != null && matchedStudent.getDemogCode().equals(StudentDemogCode.CONFIRMED.getCode())) {
      confirmedList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
    } else {
      diffList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
    }
  }

  private void populateForSystemMatchedStatus(final List<ReportListItem> sysMatchedList, final List<ReportUserMatchedListItem> diffList, final Map<String, Student> students, final PenRequestBatchStudent penRequestBatchStudent) {
    val student = students.get(penRequestBatchStudent.getStudentID());
    if (PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)) {
      sysMatchedList.add(listItemMapper.toReportListItem(penRequestBatchStudent, ""));
    } else {
      diffList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, student));
    }
  }


  private String setReviewer(final PenCoordinator penCoordinator) {
    return (penCoordinator != null && StringUtils.isNotBlank(penCoordinator.getPenCoordinatorName())) ? penCoordinator.getPenCoordinatorName() : "School PEN Coordinator";
  }

  private Map<String, Student> setStudents(final List<Student> students) {
    if (!CollectionUtils.isEmpty(students)) {
      return students.stream().collect(Collectors.toConcurrentMap(Student::getStudentID, Function.identity()));
    }
    return Collections.emptyMap();
  }
}
