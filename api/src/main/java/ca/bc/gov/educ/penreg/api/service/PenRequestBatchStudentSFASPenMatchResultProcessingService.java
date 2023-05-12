package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.SchoolTypeCode;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.BatchStudentPenMatchProcessingPayload;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The type Pen request batch student pen match result processing service.
 * this is SFAS specific implementation.
 */
@Slf4j
@Service
public class PenRequestBatchStudentSFASPenMatchResultProcessingService extends BaseBatchStudentPenMatchResultProcessingService {


  /**
   * Instantiates a new Pen request batch student pen match result processing service.
   *
   * @param penRequestBatchStudentService the pen request batch student service
   * @param restUtils                     the rest utils
   * @param penService                    the pen service
   * @param sagaService                   the saga service
   */
  public PenRequestBatchStudentSFASPenMatchResultProcessingService(final PenRequestBatchStudentService penRequestBatchStudentService, final RestUtils restUtils, final PenService penService, final SagaService sagaService) {
    super(restUtils, penService, penRequestBatchStudentService, sagaService);
  }

  /**
   * No new pens created for SFAS, and it goes to FIXABLE status.
   */
  @Override
  protected Optional<Event> handleCreateNewStudentStatus(final BatchStudentPenMatchProcessingPayload batchStudentPenMatchProcessingPayload) {
    return super.handleDefault(batchStudentPenMatchProcessingPayload);
  }

  @Override
  protected void updateStudent(String studentID, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData, PenRequestBatchEntity penRequestBatch, PenRequestBatchStudentEntity penRequestBatchStudent) {
    log.debug("SFAS flow, No updates to students.");
  }

  @Override
  public SchoolTypeCode getSchoolTypeCode() {
    return SchoolTypeCode.SFAS;
  }


}
