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
    if (data != null && !CollectionUtils.isEmpty(data.getPenRequestBatchStudents())) {
      final Map<String, Student> students = this.setStudents(data.getStudents());
      for (final PenRequestBatchStudent penRequestBatchStudent : data.getPenRequestBatchStudents()) {
        switch (Objects.requireNonNull(PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode()))) {
          case DUPLICATE:
          case ERROR:
          case REPEAT:
          case INFOREQ:
          case FIXABLE:
            val issues = data.getPenRequestBatchStudentValidationIssues().get(penRequestBatchStudent.getPenRequestBatchStudentID());
            val pendingItem = listItemMapper.toReportListItem(penRequestBatchStudent, issues);
            if (!"Duplicate".equals(pendingItem.getPen())) {
              pendingItem.setPen("Pending");
            }
            pendingList.add(pendingItem);
            break;
          case SYS_NEW_PEN:
          case USR_NEW_PEN:
            this.populateForNewPenStatus(newPenList, students, penRequestBatchStudent);
            break;
          case SYS_MATCHED:
            this.populateForSystemMatchedStatus(sysMatchedList, diffList, students, penRequestBatchStudent);
            break;
          case USR_MATCHED:
            this.populateForUserMatchedStatus(diffList, confirmedList, students, penRequestBatchStudent);
            break;
          default:
            log.error("Unexpected pen request batch student status code encountered while attempting generate report data :: " + penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
            break;
        }
      }
      val processDateTime = data.getPenRequestBatch() == null || data.getPenRequestBatch().getProcessDate() == null ? LocalDateTime.now() : LocalDateTime.parse(data.getPenRequestBatch().getProcessDate());
      reportData.setProcessDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
      reportData.setProcessTime(processDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
      reportData.setReportDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd")).toUpperCase().replace(".", ""));
      reportData.setReviewer(this.setReviewer(data.getPenCoordinator()));
    }


    reportData.setSysMatchedList(sysMatchedList);
    reportData.setPendingList(pendingList);
    reportData.setNewPenList(newPenList);
    reportData.setDiffList(diffList);
    reportData.setConfirmedList(confirmedList);
    return reportData;
  }

  private void populateForNewPenStatus(final List<ReportListItem> newPenList, final Map<String, Student> students, final PenRequestBatchStudent penRequestBatchStudent) {
    if (students.get(penRequestBatchStudent.getStudentID()) == null) {
      log.error("Error attempting to create report data. Students list should not be null for USR_NEW_PEN status.");
      return;
    }
    val item = listItemMapper.toReportListItem(students.get(penRequestBatchStudent.getStudentID()));
    // override the value here. see https://gww.jira.educ.gov.bc.ca/browse/PEN-1523
    // since student wont have full usual name , system overrides it from request student data in the batch file.
    if (StringUtils.isNotBlank(item.getUsualName())) {
      item.setUsualName(listItemMapper.populateUsualName(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()));
    }
    newPenList.add(item);
  }

  private void populateForUserMatchedStatus(final List<ReportUserMatchedListItem> diffList, final List<ReportUserMatchedListItem> confirmedList, final Map<String, Student> students, final PenRequestBatchStudent penRequestBatchStudent) {
    if (students == null || students.get(penRequestBatchStudent.getStudentID()) == null) {
      log.error("Error attempting to create report data. Students list should not be null for USR_MATCHED status.");
      return;
    }
    val matchedStudent = students.get(penRequestBatchStudent.getStudentID());
    if (matchedStudent != null && matchedStudent.getDemogCode() != null && matchedStudent.getDemogCode().equals(StudentDemogCode.CONFIRMED.getCode())) {
      val item = listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent);
      // override the value here. see https://gww.jira.educ.gov.bc.ca/browse/PEN-1523
      // since student wont have full usual name , system overrides it from request student data in the batch file.
      if (StringUtils.isNotBlank(item.getMin().getUsualName())) {
        item.getMin().setUsualName(listItemMapper.populateUsualName(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()));
      }
      confirmedList.add(item);
    } else {
      val item = listItemMapper.toReportUserMatchedDiffListItem(penRequestBatchStudent, matchedStudent);
      // override the value here. see https://gww.jira.educ.gov.bc.ca/browse/PEN-1523
      // since student wont have full usual name , system overrides it from request student data in the batch file.
      if (StringUtils.isNotBlank(item.getMin().getUsualName())) {
        item.getMin().setUsualName(listItemMapper.populateUsualName(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()));
      }
      diffList.add(item);
    }
  }

  private void populateForSystemMatchedStatus(final List<ReportListItem> sysMatchedList, final List<ReportUserMatchedListItem> diffList, final Map<String, Student> students, final PenRequestBatchStudent penRequestBatchStudent) {
    val student = students.get(penRequestBatchStudent.getStudentID());
    if (PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)) {
      val item = listItemMapper.toReportListItem(penRequestBatchStudent, "");
      // override the value here. see https://gww.jira.educ.gov.bc.ca/browse/PEN-1523
      // since student wont have full usual name , system overrides it from request student data in the batch file.
      if (StringUtils.isNotBlank(item.getUsualName())) {
        item.setUsualName(listItemMapper.populateUsualName(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()));
      }
      sysMatchedList.add(item);
    } else {
      val item = listItemMapper.toReportUserMatchedDiffListItem(penRequestBatchStudent, student);
      // override the value here. see https://gww.jira.educ.gov.bc.ca/browse/PEN-1523
      // since student wont have full usual name , system overrides it from request student data in the batch file.
      if (StringUtils.isNotBlank(item.getMin().getUsualName())) {
        item.getMin().setUsualName(listItemMapper.populateUsualName(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()));
      }
      diffList.add(item);
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
