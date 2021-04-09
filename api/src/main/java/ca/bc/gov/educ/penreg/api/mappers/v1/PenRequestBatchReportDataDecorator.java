package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.StudentDemogCode;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        Map<String, Student> students = data.getStudents().stream()
                .collect(Collectors.toMap(Student::getStudentID, student -> student));
        for(PenRequestBatchStudent penRequestBatchStudent : data.getPenRequestBatchStudents()) {
            switch (Objects.requireNonNull(PenRequestBatchStudentStatusCodes.codeOfValue(penRequestBatchStudent.getPenRequestBatchStudentStatusCode()))) {
                case DUPLICATE:
                case ERROR:
                case REPEAT:
                case INFOREQ:
                case FIXABLE:
                    pendingList.add(listItemMapper.toReportListItem(penRequestBatchStudent));
                    break;
                case SYS_NEW_PEN:
                case USR_NEW_PEN:
                    newPenList.add(listItemMapper.toReportListItem(students.get(penRequestBatchStudent.getStudentID())));
                    break;
                case SYS_MATCHED:
                    sysMatchedList.add(listItemMapper.toReportListItem(penRequestBatchStudent));
                    break;
                case USR_MATCHED:
                    Student matchedStudent = students.get(penRequestBatchStudent.getStudentID());
                    if(matchedStudent != null && matchedStudent.getDemogCode() != null && matchedStudent.getDemogCode().equals(StudentDemogCode.CONFIRMED.getCode())) {
                        confirmedList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
                    } else {
                        diffList.add(listItemMapper.toReportUserMatchedListItem(penRequestBatchStudent, matchedStudent));
                    }
                    break;
                default:
                    log.error("Unexpected pen request batch student error code encountered while attempting generate report data :: " + penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
                    break;
            }
            reportData.setSysMatchedList(sysMatchedList);
            reportData.setPendingList(pendingList);
            reportData.setNewPenList(newPenList);
            reportData.setDiffList(diffList);
            reportData.setConfirmedList(confirmedList);

            reportData.setProcessDate(LocalDateTime.parse(data.getPenRequestBatch().getProcessDate()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
            reportData.setProcessTime(LocalDateTime.parse(data.getPenRequestBatch().getProcessDate()).format(DateTimeFormatter.ofPattern("HH:mm")));
            reportData.setReportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MMM-dd")).toUpperCase().replace(".", ""));
        }
        return reportData;
    }
}
