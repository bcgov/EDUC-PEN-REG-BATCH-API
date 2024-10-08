package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes.FLAT_FILE;
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
    val isFileAlreadyProcessed = result.stream().anyMatch(this::submissionProcessedPredicate);
    if (!isFileAlreadyProcessed) {
      this.getPenRequestBatchService().savePenRequestBatch(penRequestBatchEntity);
      penWebBlobEntity.setExtractDateTime(LocalDateTime.now()); // update the entity extract date time to mark the batch job as complete , so that wont be polled from table in the next schedule.
      this.getPenWebBlobRepository().save(penWebBlobEntity);
    } else {
      // this could happen when a scheduler picks up a record from pen web blob as extract date might not have been updated as it was being processed from the previous batch.
      // we just log it for analysis purpose.
      log.warn("submission number :: {} already processed", penRequestBatchEntity.getSubmissionNumber());
    }

  }

  private boolean submissionProcessedPredicate(final PenRequestBatchEntity penRequestBatchEntity) {
    return FLAT_FILE.getCode().equals(penRequestBatchEntity.getPenRequestBatchProcessTypeCode()) && "PEN".equals(penRequestBatchEntity.getFileType());
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
   * Gets all not extracted records.
   *
   * @param fileType the file type
   * @return the all not extracted records
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<PENWebBlobEntity> getAllPenWebNotExtractedRecords(@NonNull final String fileType) {
    return this.getPenWebBlobRepository().findAllByExtractDateTimeIsNullAndFileTypeAndSourceApplication(fileType, "PENWEB");
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
      val duplicateEntity = entityMap.get(hashKey);
      if (duplicateEntity != null) {
        entity.setPenRequestBatchStudentStatusCode(PenRequestBatchStudentStatusCodes.DUPLICATE.getCode());
        filteredStudentEntities.remove(duplicateEntity); // if it is duplicate , remove the earlier record.
        duplicateEntity.setPenRequestBatchStudentStatusCode(PenRequestBatchStudentStatusCodes.DUPLICATE.getCode());
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
    return entity.getLegalLastName() + entity.getLegalFirstName() + entity.getLegalMiddleNames() + entity.getDob() + entity.getUsualFirstName() + entity.getUsualLastName() + entity.getUsualMiddleNames() + entity.getGenderCode() + entity.getGradeCode() + entity.getPostalCode();
  }

  private void checkBatchForRepeatRequests(String guid, PenRequestBatchEntity penRequestBatchEntity, Set<PenRequestBatchStudentEntity> studentEntities, Set<PenRequestBatchStudentEntity> filteredStudentEntities) {
    long numRepeats = 0;
    final Map<String, List<PenRequestBatchStudentEntity>> repeatCheckMap =
      this.penRequestBatchStudentService.populateRepeatCheckMap(penRequestBatchEntity);
    if (repeatCheckMap.size() > 0) {
      for (final PenRequestBatchStudentEntity penRequestBatchStudent : studentEntities) {
        if (PenRequestBatchStudentStatusCodes.DUPLICATE.getCode().equals(penRequestBatchStudent.getPenRequestBatchStudentStatusCode())) {
          continue; // no need to do any checking for DUPLICATE student requests, continue further.
        }
        final String repeatCheckKey =
          this.penRequestBatchStudentService.constructKeyGivenBatchStudent(penRequestBatchStudent);
        if (repeatCheckMap.containsKey(repeatCheckKey)) {
          filteredStudentEntities.remove(penRequestBatchStudent); // if it is a repeat remove it from the list to be further processed.
          this.updatePenRequestBatchStudentRequest(repeatCheckMap.get(repeatCheckKey), penRequestBatchStudent);
          numRepeats++;
        }
      }
    }
    log.debug("{} :: Found {} total repeats", guid, numRepeats);
    penRequestBatchEntity.setRepeatCount(numRepeats);
  }

  /**
   * Filter out repeat requests
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the list of filtered requests
   */
  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Set<PenRequestBatchStudentEntity> filterDuplicatesAndRepeatRequests(@NonNull final String guid, final PenRequestBatchEntity penRequestBatchEntity) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final var filteredStudentEntities = new HashSet<PenRequestBatchStudentEntity>();
    final Optional<PenRequestBatchEntity> penRequestBatchEntityOptional =
      this.findEntity(penRequestBatchEntity.getPenRequestBatchID()); // need to grab it from DB, so that this
    // becomes an attached hibernate entity, to this current transactional context.
    if (penRequestBatchEntityOptional.isPresent()) {
      final PenRequestBatchEntity penRequestBatchEntityFromDB = penRequestBatchEntityOptional.get();
      final Set<PenRequestBatchStudentEntity> studentEntities =
        penRequestBatchEntityFromDB.getPenRequestBatchStudentEntities(); // it will make a DB call here, get the
      // child entities lazily loaded.

      this.checkBatchForDuplicateRequests(studentEntities, filteredStudentEntities);

      if(!penRequestBatchEntity.getMincode().equals("10200030")) {
        this.checkBatchForRepeatRequests(guid, penRequestBatchEntityFromDB, studentEntities, filteredStudentEntities);
      }
      penRequestBatchEntityFromDB.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.REPEATS_CHECKED.getCode());
      this.getPenRequestBatchService().savePenRequestBatch(penRequestBatchEntityFromDB); // finally update the
    }
    stopwatch.stop();
    log.info("completed the method in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
      penRequestBatchStudent.setRepeatRequestSequenceNumber(mostRecentRepeat.getRepeatRequestSequenceNumber() == null ? 1 : mostRecentRepeat.getRepeatRequestSequenceNumber() + 1);
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

  /**
   * this is used to associate the object with current thread as hibernate attached entity for lazy loading.
   *
   * @param penWebBlobId the pkl of the table.
   * @return the entity if present
   */
  public Optional<PENWebBlobEntity> getPenWebBlob(final long penWebBlobId) {
    return this.penWebBlobRepository.findById(penWebBlobId);
  }
}
