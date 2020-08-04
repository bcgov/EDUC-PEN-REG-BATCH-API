package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch file service.
 *
 * @author OM
 */
@Service
@Slf4j
public class PenRequestBatchFileService {

  @Getter(PRIVATE)
  private final PenRequestBatchService penRequestBatchService;

  @Getter(PRIVATE)
  private final PenWebBlobRepository penWebBlobRepository;


  /**
   * Instantiates a new Pen request batch file service.
   *
   * @param penRequestBatchService the pen request batch service
   * @param penWebBlobRepository   the pen web blob repository
   */
  @Autowired
  public PenRequestBatchFileService(PenRequestBatchService penRequestBatchService, PenWebBlobRepository penWebBlobRepository) {
    this.penRequestBatchService = penRequestBatchService;
    this.penWebBlobRepository = penWebBlobRepository;
  }

  /**
   * Save pen request batch entity pen request batch entity.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @param penWebBlobEntity      the pen web blob entity, <b> make sure the entity passed here is a hibernate attached entity</b>
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void markInitialLoadComplete(@NonNull final PenRequestBatchEntity penRequestBatchEntity, @NonNull final PENWebBlobEntity penWebBlobEntity) {
    var result = getPenRequestBatchService().findPenRequestBatchBySubmissionNumber(penRequestBatchEntity.getSubmissionNumber());
    if (result.isEmpty()) {
      getPenRequestBatchService().createPenRequestBatch(penRequestBatchEntity);
      penWebBlobEntity.setExtractDateTime(LocalDateTime.now()); // update the entity extract date time to mark the batch job as complete , so that wont be polled from table in the next schedule.
      getPenWebBlobRepository().save(penWebBlobEntity);
    } else {
      // this could happen when a scheduler picks up a record from pen web blob as extract date might not have been updated as it was being processed from the previous batch.
      // we just log it for analysis purpose.
      log.warn("submission number :: {} already processed", penRequestBatchEntity.getSubmissionNumber());
    }

  }

  /**
   * Gets all not extracted records.
   *
   * @return the all not extracted records
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<PENWebBlobEntity> getAllNotExtractedRecords() {
    return getPenWebBlobRepository().findAllByExtractDateTimeIsNull();
  }
}
