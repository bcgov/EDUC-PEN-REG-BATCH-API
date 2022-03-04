package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.SchoolTypeCode;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The type Pen request batch student pen match result processing service.
 */
@Slf4j
@Service
public class PenRequestBatchStudentPSIPenMatchResultProcessingService extends BaseBatchStudentPenMatchResultProcessingService {


  /**
   * Instantiates a new Pen request batch student pen match result processing service.
   *
   * @param penRequestBatchStudentService the pen request batch student service
   * @param restUtils                     the rest utils
   * @param penService                    the pen service
   * @param sagaService                   the saga service
   */
  public PenRequestBatchStudentPSIPenMatchResultProcessingService(final PenRequestBatchStudentService penRequestBatchStudentService, final RestUtils restUtils, final PenService penService, final SagaService sagaService) {
    super(restUtils, penService, penRequestBatchStudentService, sagaService);
  }


  @Override
  public SchoolTypeCode getSchoolTypeCode() {
    return SchoolTypeCode.PSI;
  }

}
