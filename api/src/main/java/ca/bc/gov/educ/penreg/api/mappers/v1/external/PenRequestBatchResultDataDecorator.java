package ca.bc.gov.educ.penreg.api.mappers.v1.external;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.StudentDemogCode;
import ca.bc.gov.educ.penreg.api.mappers.PenRequestBatchStudentValidationIssueMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.external.ListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmissionResult;
import ca.bc.gov.educ.penreg.api.struct.v1.external.SchoolMinListItem;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public abstract class PenRequestBatchResultDataDecorator implements PenRequestBatchResultDataMapper {

  private static final ListItemMapper listItemMapper = ListItemMapper.mapper;
  /**
   * The Delegate
   */
  private final PenRequestBatchResultDataMapper delegate;


  protected PenRequestBatchResultDataDecorator(final PenRequestBatchResultDataMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public PenRequestBatchSubmissionResult toResult(final PenRequestBatchEntity penRequestBatch, final Map<String, Student> studentMap) {
    final var requestBatchSubmissionResult = this.delegate.toResult(penRequestBatch, studentMap);
    final List<ListItem> pendingList = new ArrayList<>();
    final List<ListItem> newPenAssignedList = new ArrayList<>();
    final List<ListItem> exactMatchList = new ArrayList<>();
    final List<SchoolMinListItem> differencesList = new ArrayList<>();
    final List<SchoolMinListItem> confirmedList = new ArrayList<>();
    for (final PenRequestBatchStudentEntity penRequestBatchStudent : penRequestBatch.getPenRequestBatchStudentEntities()) {
      switch (Objects.requireNonNull(PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode()))) {
        case DUPLICATE:
        case ERROR:
        case REPEAT:
        case INFOREQ:
        case FIXABLE:
          val fixableItem = listItemMapper.toListItem(penRequestBatchStudent);
          fixableItem.setValidationIssues(penRequestBatchStudent.getPenRequestBatchStudentValidationIssueEntities().stream().filter(validationResult -> "ERROR".equals(validationResult.getPenRequestBatchValidationIssueSeverityCode())).map(PenRequestBatchStudentValidationIssueMapper.mapper::toStruct).collect(Collectors.toList()));
          pendingList.add(fixableItem);
          break;
        case SYS_NEW_PEN:
        case USR_NEW_PEN:
          if (studentMap.get(penRequestBatchStudent.getStudentID().toString()) == null) {
            log.error("Error attempting to PenRequestBatchSubmissionResult data. Students list should not be null for USR_NEW_PEN or SYS_NEW_PEN status.");
            break;
          }
          newPenAssignedList.add(listItemMapper.toListItem(studentMap.get(penRequestBatchStudent.getStudentID().toString())));
          break;
        case SYS_MATCHED:
          exactMatchList.add(listItemMapper.toListItem(studentMap.get(penRequestBatchStudent.getStudentID().toString())));
          break;
        case USR_MATCHED:
          if (studentMap.get(penRequestBatchStudent.getStudentID().toString()) == null) {
            log.error("Error attempting to create report data. Students list should not be null for USR_MATCHED status.");
            break;
          }
          val matchedStudent = studentMap.get(penRequestBatchStudent.getStudentID().toString());
          if (matchedStudent != null && matchedStudent.getDemogCode() != null && matchedStudent.getDemogCode().equals(StudentDemogCode.CONFIRMED.getCode())) {
            val item = new SchoolMinListItem();
            item.setMin(listItemMapper.toListItem(matchedStudent));
            item.setSchool(listItemMapper.toListItem(penRequestBatchStudent));
            confirmedList.add(item);
          } else {
            val item = new SchoolMinListItem();
            item.setMin(listItemMapper.toListItem(matchedStudent));
            item.setSchool(listItemMapper.toListItem(penRequestBatchStudent));
            differencesList.add(item);
          }
          break;
        default:
          log.error("Unexpected pen request batch student error code encountered while attempting generate PenRequestBatchSubmissionResult data :: " + penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
          break;
      }
    }
    requestBatchSubmissionResult.setPendingList(pendingList);
    requestBatchSubmissionResult.setConfirmedList(confirmedList);
    requestBatchSubmissionResult.setDifferencesList(differencesList);
    requestBatchSubmissionResult.setExactMatchList(exactMatchList);
    requestBatchSubmissionResult.setNewPenAssignedList(newPenAssignedList);
    return requestBatchSubmissionResult;
  }

}
