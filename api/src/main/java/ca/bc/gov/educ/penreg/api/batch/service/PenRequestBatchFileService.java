package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
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
import java.util.*;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch file service.
 *
 * @author OM
 */
@Service
@Slf4j
public class PenRequestBatchFileService {

  /**
   * The Pen request batch service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchService penRequestBatchService;

  /**
   * The Pen request batch student service
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentService penRequestBatchStudentService;

  /**
   * The Pen web blob repository.
   */
  @Getter(PRIVATE)
  private final PenWebBlobRepository penWebBlobRepository;


  /**
   * Instantiates a new Pen request batch file service.
   *
   * @param penRequestBatchService        the pen request batch service
   * @param penRequestBatchStudentService the pen request batch student service
   * @param penWebBlobRepository          the pen web blob repository
   */
  @Autowired
  public PenRequestBatchFileService(final PenRequestBatchService penRequestBatchService, final PenRequestBatchStudentService penRequestBatchStudentService, final PenWebBlobRepository penWebBlobRepository) {
    this.penRequestBatchService = penRequestBatchService;
    this.penRequestBatchStudentService = penRequestBatchStudentService;
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
    final var result = this.getPenRequestBatchService().findPenRequestBatchBySubmissionNumber(penRequestBatchEntity.getSubmissionNumber());
    if (result.isEmpty()) {
      this.getPenRequestBatchService().savePenRequestBatch(penRequestBatchEntity);
      penWebBlobEntity.setExtractDateTime(LocalDateTime.now()); // update the entity extract date time to mark the batch job as complete , so that wont be polled from table in the next schedule.
      this.getPenWebBlobRepository().save(penWebBlobEntity);
    } else {
      // this could happen when a scheduler picks up a record from pen web blob as extract date might not have been updated as it was being processed from the previous batch.
      // we just log it for analysis purpose.
      log.warn("submission number :: {} already processed", penRequestBatchEntity.getSubmissionNumber());
    }

  }

  /**
   * Gets all not extracted records.
   *
   * @param fileType the file type
   * @return the all not extracted records
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<PENWebBlobEntity> getAllNotExtractedRecords(@NonNull final String fileType) {
    return this.getPenWebBlobRepository().findAllByExtractDateTimeIsNullAndFileType(fileType);
  }

  /**
   * Check a set of pen request batch student entities for duplicates
   *
   * @param studentEntities - the set of entities to check
   */
  private void checkBatchForDuplicateRequests(final Set<PenRequestBatchStudentEntity> studentEntities, final Set<PenRequestBatchStudentEntity> filteredStudentEntities) {
    final Map<String, PenRequestBatchStudentEntity> entityMap = new HashMap<>();
    studentEntities.forEach(entity -> {
      final var hashKey = this.constructKeyForDuplicateEntity(entity);
      if (entityMap.containsKey(hashKey)) {
        entity.setPenRequestBatchStudentStatusCode(PenRequestBatchStudentStatusCodes.DUPLICATE.getCode());
        this.getPenRequestBatchStudentService().saveAttachedEntity(entity);
        studentEntities.remove(entity);
      } else {
        entityMap.put(hashKey, entity);
        filteredStudentEntities.add(entity);
      }
    });
  }

  /**
   * Uses the logic for when a student request is a duplicate to construct a key for using in hash map
   *
   * @param entity - the entity
   * @return - the key
   */
  private String constructKeyForDuplicateEntity(final PenRequestBatchStudentEntity entity) {
    return entity.getLegalLastName() + entity.getLegalFirstName() + entity.getDob();
  }

  /**
   * Filter out repeat requests
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the list of filtered requests
   */
  public Set<PenRequestBatchStudentEntity> filterDuplicatesAndRepeatRequests(@NonNull final String guid, final PenRequestBatchEntity penRequestBatchEntity) {
    final Set<PenRequestBatchStudentEntity> studentEntities = penRequestBatchEntity.getPenRequestBatchStudentEntities();
    long numRepeats = 0;
    final var filteredStudentEntities = new HashSet<PenRequestBatchStudentEntity>();

    this.checkBatchForDuplicateRequests(studentEntities, filteredStudentEntities);

    for (final PenRequestBatchStudentEntity penRequestBatchStudent : studentEntities) {
      final List<PenRequestBatchStudentEntity> repeatRequests = this.penRequestBatchStudentService.findAllRepeatsGivenBatchStudent(penRequestBatchStudent);
      log.trace("{} :: Checking following penRequestBatchStudent for repeats :: {}", guid, penRequestBatchStudent);
      log.debug("{} :: Found {} repeat records for prb student record :: {}", guid, repeatRequests.size(), penRequestBatchStudent.getPenRequestBatchStudentID());
      if (!repeatRequests.isEmpty()) {
        this.updatePenRequestBatchStudentRequest(repeatRequests, penRequestBatchStudent);
        filteredStudentEntities.remove(penRequestBatchStudent);
        numRepeats++;
      } else {
        filteredStudentEntities.add(penRequestBatchStudent);
      }
    }
    log.debug("{} :: Found {} total repeats", guid, numRepeats);
    penRequestBatchEntity.setRepeatCount(numRepeats);
    penRequestBatchEntity.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.REPEATS_CHECKED.getCode());
    this.getPenRequestBatchService().savePenRequestBatch(penRequestBatchEntity);

    return filteredStudentEntities;
  }

  /**
   * Update student batch requests to mark them as repeats
   *
   * @param repeatRequests         the list of previous identical requests
   * @param penRequestBatchStudent the request to be updated
   */
  private void updatePenRequestBatchStudentRequest(final List<PenRequestBatchStudentEntity> repeatRequests, final PenRequestBatchStudentEntity penRequestBatchStudent) {
    if (repeatRequests.size() > 1) {
      final PenRequestBatchStudentEntity mostRecentRepeat = Collections.max(repeatRequests, Comparator.comparing(t -> t.getPenRequestBatchEntity().getProcessDate()));
      penRequestBatchStudent.setRepeatRequestOriginalID(mostRecentRepeat.getRepeatRequestOriginalID());
      penRequestBatchStudent.setRepeatRequestSequenceNumber(mostRecentRepeat.getRepeatRequestSequenceNumber() == null? 1 : mostRecentRepeat.getRepeatRequestSequenceNumber() + 1);
    } else {
      penRequestBatchStudent.setRepeatRequestOriginalID(repeatRequests.get(0).getRepeatRequestOriginalID());
      penRequestBatchStudent.setRepeatRequestSequenceNumber(1);
    }
    penRequestBatchStudent.setPenRequestBatchStudentStatusCode(PenRequestBatchStudentStatusCodes.REPEAT.getCode());
  }

  /**
   * Find entity optional.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the optional
   */
  public Optional<PenRequestBatchEntity> findEntity(final UUID penRequestBatchID) {
    return this.getPenRequestBatchService().findById(penRequestBatchID);
  }
}
