package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.SchoolTypeCode;
import ca.bc.gov.educ.penreg.api.constants.StudentHistoryActivityCode;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.BatchStudentPenMatchProcessingPayload;
import ca.bc.gov.educ.penreg.api.util.LocalIDUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * The type Pen request batch student pen match result processing service.
 * this is summer school specific implementation.
 */
@Slf4j
@Service
public class PenRequestBatchStudentSummerSchoolPenMatchResultProcessingService extends BaseBatchStudentPenMatchResultProcessingService {


  /**
   * Instantiates a new Pen request batch student pen match result processing service.
   *
   * @param penRequestBatchStudentService the pen request batch student service
   * @param restUtils                     the rest utils
   * @param penService                    the pen service
   * @param sagaService                   the saga service
   */
  public PenRequestBatchStudentSummerSchoolPenMatchResultProcessingService(final PenRequestBatchStudentService penRequestBatchStudentService, final RestUtils restUtils, final PenService penService, final SagaService sagaService) {
    super(restUtils, penService, penRequestBatchStudentService, sagaService);
  }

  /**
   * https://gww.wiki.educ.gov.bc.ca/pages/viewpage.action?pageId=55027476
   * only update Mincode and LocalID for matched student.
   */
  @Override
  protected void updateStudentData(final Student studentFromStudentAPI, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData, final PenRequestBatchEntity penRequestBatchEntity, final PenRequestBatchStudentEntity penRequestBatchStudent) {
    log.debug("Summer school flow, only updating mincode and local id.");
    studentFromStudentAPI.setMincode(penRequestBatchEntity.getMincode());
    // updated as part of https://gww.jira.educ.gov.bc.ca/browse/PEN-1347
    final var changesBadLocalIDIfExistBeforeSetValue = LocalIDUtil.changeBadLocalID(StringUtils.remove(penRequestBatchStudentSagaData.getLocalID(), ' '));
    studentFromStudentAPI.setLocalID(changesBadLocalIDIfExistBeforeSetValue);
    studentFromStudentAPI.setHistoryActivityCode(StudentHistoryActivityCode.REQ_MATCH.getCode());
    studentFromStudentAPI.setUpdateUser(ALGORITHM);
  }

  /**
   * No new pens created for summer school, and it goes to FIXABLE status.
   */
  @Override
  protected Optional<Event> handleCreateNewStudentStatus(final BatchStudentPenMatchProcessingPayload batchStudentPenMatchProcessingPayload) {
    return super.handleDefault(batchStudentPenMatchProcessingPayload);
  }

  @Override
  public SchoolTypeCode getSchoolTypeCode() {
    return SchoolTypeCode.SUMMER_SCHOOL;
  }


}
