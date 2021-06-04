package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.StudentDemogCode;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class PenRequestBatchReportDataDecorator implements PenRequestBatchReportDataMapper {

    /**
     * The Delegate
     */
    private final PenRequestBatchReportDataMapper delegate;

    private static final ReportListItemMapper listItemMapper = ReportListItemMapper.mapper;

    protected PenRequestBatchReportDataDecorator(final PenRequestBatchReportDataMapper mapper) {
        this.delegate = mapper;
    }

    /**
     * To pen request batch student entity pen request batch student entity.
     *
     * @param data        the saga data
     * @return the pen request batch report data entity
     */
    @Override
    public PenRequestBatchReportData toReportData(final BasePenRequestBatchReturnFilesSagaData data) {
      final var reportData = this.delegate.toReportData(data);
      List<ReportListItem> pendingList = new ArrayList<>();
      List<ReportListItem> newPenList = new ArrayList<>();
      List<ReportListItem> sysMatchedList = new ArrayList<>();
      List<ReportUserMatchedListItem> diffList = new ArrayList<>();
      List<ReportUserMatchedListItem> confirmedList = new ArrayList<>();
      Map<String, Student> students = this.setStudents(data.getStudents());

      for (PenRequestBatchStudent penRequestBatchStudent : data.getPenRequestBatchStudents()) {
        switch (Objects.requireNonNull(PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode()))) {
          case DUPLICATE:
          case ERROR:
          case REPEAT:
          case INFOREQ:
          case FIXABLE:
            pendingList.add(listItemMapper.toReportListItem(penRequestBatchStudent));
            break;
          case SYS_NEW_PEN:
          case USR_NEW_PEN:
            if (students.get(penRequestBatchStudent.getStudentID()) == null) {
              log.error("Error attempting to create report data. Students list should not be null for USR_NEW_PEN status.");
              break;
            }
            newPenList.add(listItemMapper.toReportListItem(students.get(penRequestBatchStudent.getStudentID())));
            break;
          case SYS_MATCHED:
            sysMatchedList.add(listItemMapper.toReportListItem(penRequestBatchStudent));
            break;
          case USR_MATCHED:
            if (students.get(penRequestBatchStudent.getStudentID()) == null) {
              log.error("Error attempting to create report data. Students list should not be null for USR_MATCHED status.");
              break;
            }
            Student matchedStudent = students.get(penRequestBatchStudent.getStudentID());
            if (matchedStudent != null && matchedStudent.getDemogCode() != null && matchedStudent.getDemogCode().equals(StudentDemogCode.CONFIRMED.getCode())) {
              confirmedList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
            } else {
              diffList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
            }
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

      var processDateTime = LocalDateTime.parse(data.getPenRequestBatch().getProcessDate());
      reportData.setProcessDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
      reportData.setProcessTime(processDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
      reportData.setReportDate(processDateTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd")).toUpperCase().replace(".", ""));

      reportData.setReviewer(this.setReviewer(data.getPenCoordinator()));

      return reportData;
    }

    private String setReviewer(PenCoordinator penCoordinator) {
      String penCoordinatorName = "School PEN Coordinator";
      if (penCoordinator != null && StringUtils.isNotBlank(penCoordinator.getPenCoordinatorName())) {
        penCoordinatorName = penCoordinator.getPenCoordinatorName();
      }
      return penCoordinatorName;
    }

    private Map<String, Student> setStudents (List<Student> students) {
      if (!CollectionUtils.isEmpty(students)) {
        return students.stream().collect(Collectors.toMap(Student::getStudentID, student -> student));
      }
      return Collections.emptyMap();
    }
}
