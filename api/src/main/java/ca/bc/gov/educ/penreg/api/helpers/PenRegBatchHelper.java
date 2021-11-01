package ca.bc.gov.educ.penreg.api.helpers;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolTypeCode;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class to provide some common methods that can be called from components, as a single source of truth for
 * easier maintainable code.
 */
@Slf4j
public final class PenRegBatchHelper {
  private static final Set<String> statusesMeansToComplete = new HashSet<>();

  static {
    statusesMeansToComplete.add(PenRequestBatchStatusCodes.LOAD_FAIL.getCode());
    statusesMeansToComplete.add(PenRequestBatchStatusCodes.ARCHIVED.getCode());
    statusesMeansToComplete.add(PenRequestBatchStatusCodes.REARCHIVED.getCode());
  }

  private PenRegBatchHelper() {

  }

  public static PenRequestBatchStudentSagaData createSagaDataFromStudentRequestAndBatch(@NonNull final PenRequestBatchStudentEntity penRequestBatchStudentEntity, @NonNull final PenRequestBatchEntity penRequestBatchEntity) {
    val sagaData = PenRegBatchHelper.convertPRBStudentToSagaData(penRequestBatchStudentEntity);
    return PenRegBatchHelper.updateEachSagaRecord(sagaData, penRequestBatchEntity);
  }

  public static Set<PenRequestBatchStudentSagaData> createSagaDataFromStudentRequestsAndBatch(@NonNull final Set<PenRequestBatchStudentEntity> penRequestBatchStudentEntities, @NonNull final PenRequestBatchEntity penRequestBatchEntity) {
    return penRequestBatchStudentEntities.stream()
      .map(PenRegBatchHelper::convertPRBStudentToSagaData)
      .map(element -> updateEachSagaRecord(element, penRequestBatchEntity))
      .collect(Collectors.toSet());
  }

  private static PenRequestBatchStudentSagaData convertPRBStudentToSagaData(@NonNull final PenRequestBatchStudentEntity penRequestBatchStudentEntity) {
    return PenRequestBatchStudentSagaDataMapper.mapper.toPenReqBatchStudentSagaData(penRequestBatchStudentEntity);
  }

  private static PenRequestBatchStudentSagaData updateEachSagaRecord(final PenRequestBatchStudentSagaData element,
                                                                     final PenRequestBatchEntity penRequestBatchEntity) {
    element.setMincode(penRequestBatchEntity.getMincode());
    element.setPenRequestBatchID(penRequestBatchEntity.getPenRequestBatchID());
    return element;
  }

  public static boolean isPRBStatusConsideredComplete(final String statusCode) {
    return statusesMeansToComplete.contains(statusCode);
  }

  /**
   * below is the old logic which needs to be written in java, it is exact match if it satisfies below condition
   * <pre>
   *    IF STUD_SURNAME  OF PEN_DEMOG = LEGAL_SURNAME     OF PEN_WORK_STUD
   *    AND &         STUD_GIVEN    OF PEN_DEMOG = LEGAL_GIVEN_NAME  OF PEN_WORK_STUD
   *    AND &         STUD_MIDDLE   OF  PEN_DEMOG = LEGAL_MIDDLE_NAME OF PEN_WORK_STUD
   *    AND &         STUD_BIRTH    OF PEN_DEMOG = BIRTHDATE         OF PEN_WORK_STUD
   *    AND &         STUD_SEX      OF PEN_DEMOG = GENDER            OF PEN_WORK_STUD
   *    AND &         (ORIG_STUDENT_ID OF PEN_WORK_STUD = " " OR &   ORIG_STUDENT_ID OF PEN_WORK_STUD = STUDENT_ID OF PEN_WORK_STUD)
   * </pre>
   *
   * @param penRequestBatchStudent the student data from school/psi.
   * @param student                the student data at ministry.
   * @return if exact match true else false.
   */
  public static boolean exactMatch(final PenRequestBatchStudent penRequestBatchStudent, final Student student) {
    if(penRequestBatchStudent == null || student == null) {
      return false;
    }
    return StringUtils.equalsIgnoreCase(penRequestBatchStudent.getLegalLastName(), student.getLegalLastName())
      && StringUtils.equalsIgnoreCase(penRequestBatchStudent.getLegalFirstName(), student.getLegalFirstName())
      && StringUtils.equalsIgnoreCase(penRequestBatchStudent.getLegalMiddleNames(), student.getLegalMiddleNames())
      && StringUtils.equalsIgnoreCase(penRequestBatchStudent.getDob(), RegExUtils.removeAll(student.getDob(), "[^\\d]"))
      && StringUtils.equalsIgnoreCase(penRequestBatchStudent.getGenderCode(), student.getSexCode())
      && (StringUtils.isBlank(penRequestBatchStudent.getSubmittedPen())
      || StringUtils.equalsIgnoreCase(penRequestBatchStudent.getSubmittedPen(), penRequestBatchStudent.getAssignedPEN()));
  }

  /**
   * this method determines what is the school group code based on different parameters.
   */
  public static SchoolTypeCode getSchoolTypeCodeFromMincode(String mincode) {
    if (StringUtils.isBlank(mincode)) {
      return SchoolTypeCode.DEFAULT;
    }
    if (StringUtils.equals(StringUtils.substring(mincode, 3, 5), "90")) {
      return SchoolTypeCode.SUMMER_SCHOOL;
    } else if (mincode.equals("10200030")) {
      return SchoolTypeCode.SFAS;
    }
    return SchoolTypeCode.DEFAULT;
  }
}
