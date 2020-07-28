package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch file service.
 * @author OM
 */
@Service
public class PenRequestBatchFileService {

  @Getter(PRIVATE)
  private final PenRequestBatchService penRequestBatchService;


  @Getter(PRIVATE)
  private final PenRequestBatchStudentService penRequestBatchStudentService;

  @Autowired
  public PenRequestBatchFileService(PenRequestBatchService penRequestBatchService, PenRequestBatchStudentService penRequestBatchStudentService) {
    this.penRequestBatchService = penRequestBatchService;
    this.penRequestBatchStudentService = penRequestBatchStudentService;
  }

  /**
   * Save pen request batch entity pen request batch entity.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch entity
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public PenRequestBatchEntity savePenRequestBatchEntity(final PenRequestBatchEntity penRequestBatchEntity) {
    return getPenRequestBatchService().createPenRequestBatch(penRequestBatchEntity);
  }
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Iterable<PenRequestBatchStudentEntity> savePenRequestBatchStudentEntities(final List<PenRequestBatchStudentEntity> penRequestBatchStudentEntities) {
    return getPenRequestBatchStudentService().saveAllStudents(penRequestBatchStudentEntities);
  }
}
