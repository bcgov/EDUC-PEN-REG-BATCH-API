package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchStudentRecordsProcessor;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.penreg.api.helpers.LogHelper;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.model.v1.*;
import ca.bc.gov.educ.penreg.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REPEATS_CHECKED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.DUPLICATE;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.REPEAT;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler async service.
 */
@Service
@Slf4j
public class EventTaskSchedulerAsyncService {

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchHistoryMapper historyMapper = PenRequestBatchHistoryMapper.mapper;
  /**
   * The String redis template.
   */
  private final StringRedisTemplate stringRedisTemplate;
  /**
   * The Saga repository.
   */
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;
  /**
   * The Pen request batch repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchRepository penRequestBatchRepository;
  /**
   * The Pen request batch student repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentRepository penRequestBatchStudentRepository;
  /**
   * The Pen reg batch student records processor.
   */
  @Getter(PRIVATE)
  private final PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor;
  /**
   * The saga name and orchestrator map.
   */
  @Getter(PRIVATE)
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();
  /**
   * The pen request batch event repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchEventRepository penRequestBatchEventRepository;
  /**
   * The Status filters.
   */
  @Setter
  private List<String> statusFilters;

  /**
   * Instantiates a new Event task scheduler async service.
   *
   * @param sagaRepository                     the saga repository
   * @param penRequestBatchRepository          the pen request batch repository
   * @param penRequestBatchStudentRepository   the pen request batch student repository
   * @param penRegBatchStudentRecordsProcessor the pen reg batch student records processor
   * @param penRequestBatchEventRepository     the pen request batch event repository
   * @param orchestrators                      the orchestrators
   * @param redisConnectionFactory             the redis template
   */
  public EventTaskSchedulerAsyncService(final SagaRepository sagaRepository, final PenRequestBatchRepository penRequestBatchRepository,
                                        final PenRequestBatchStudentRepository penRequestBatchStudentRepository,
                                        final PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor,
                                        final PenRequestBatchEventRepository penRequestBatchEventRepository,
                                        final List<Orchestrator> orchestrators,
                                        final RedisConnectionFactory redisConnectionFactory) {
    this.sagaRepository = sagaRepository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
    this.penRequestBatchEventRepository = penRequestBatchEventRepository;
    this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
    orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  /**
   * Mark processed batches active or archived.
   * Redis is used to make sure the job is not updating the same record twice, if there is a delay in processing.
   */
  @Async("taskExecutor")
  @Transactional
  public void markProcessedBatchesActiveOrArchived() {
    final var penReqBatches = this.getPenRequestBatchRepository().findByPenRequestBatchStatusCode(REPEATS_CHECKED.getCode());
    log.debug("found {} records in repeat checked state", penReqBatches.size());
    if (!penReqBatches.isEmpty()) {
      final var penReqBatchEntities = new ArrayList<PenRequestBatchEntity>();
      for (final var penRequestBatchEntity : penReqBatches) {
        final String redisKey = penRequestBatchEntity.getPenRequestBatchID().toString().concat(
          "::markProcessedBatchesActive");
        val valueFromRedis = this.stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isBlank(valueFromRedis)) { // skip if it is already in redis
          this.checkAndUpdateStatusToActiveOrArchived(penReqBatchEntities, penRequestBatchEntity, redisKey);
        }
      }
      if (!penReqBatchEntities.isEmpty()) {
        log.info("marking {} records ACTIVE or ARCHIVED", penReqBatchEntities.size());
        this.getPenRequestBatchRepository().saveAll(penReqBatchEntities); // update all of them in one commit.
      }
    }
  }

  /**
   * Check and update status to active.
   *
   * @param penReqBatchEntities   the pen req batch entities
   * @param penRequestBatchEntity the pen request batch entity
   * @param redisKey              the redis key
   */
  private void checkAndUpdateStatusToActiveOrArchived(final List<PenRequestBatchEntity> penReqBatchEntities, final PenRequestBatchEntity penRequestBatchEntity, final String redisKey) {

    final var studentSagaRecords = this.getSagaRepository().findByPenRequestBatchIDAndSagaName(penRequestBatchEntity.getPenRequestBatchID(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString());
    long dupCount = this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(DUPLICATE.getCode()));
    long rptCount = this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(REPEAT.getCode()));
    long newPenAndMatchCount = this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode()));

    final List<PenRequestBatchMultiplePen> recordWithMultiples = this.penRequestBatchStudentRepository.findBatchFilesWithMultipleAssignedPens(List.of(penRequestBatchEntity.getPenRequestBatchID()));
    boolean isSamePenAssignedToMultiplePRForSameBatch = !recordWithMultiples.isEmpty();

    if (penRequestBatchEntity.getStudentCount() == (rptCount + dupCount)) { // all records are either repeat or
      this.handleAllDuplicateOrRepeat(penReqBatchEntities, penRequestBatchEntity, redisKey);
    } else if (!studentSagaRecords.isEmpty()) {
      this.updateBasedOnCompletedSagas(penReqBatchEntities, penRequestBatchEntity, redisKey, studentSagaRecords, newPenAndMatchCount, isSamePenAssignedToMultiplePRForSameBatch);
    }
  }

  private void updateBasedOnCompletedSagas(final List<PenRequestBatchEntity> penReqBatchEntities, final PenRequestBatchEntity penRequestBatchEntity, final String redisKey, final List<Saga> studentSagaRecords, final long newPenAndMatchCount, final boolean isSamePenAssignedToMultiplePRForSameBatch) {
    final long completedCount = this.sagaRepository.countAllByPenRequestBatchIDAndSagaNameAndStatus(penRequestBatchEntity.getPenRequestBatchID(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), SagaStatusEnum.COMPLETED.toString());
    if (completedCount == studentSagaRecords.size()) { // All records are processed mark batch to active.
      this.setDifferentCounts(penRequestBatchEntity);
      if (newPenAndMatchCount == completedCount && !isSamePenAssignedToMultiplePRForSameBatch) { // all records are either New PEN by System or Matched by System and same pen is not assigned to more than one student for the batch.
        final var sagas = this.getSagaRepository().findByPenRequestBatchIDAndSagaName(penRequestBatchEntity.getPenRequestBatchID(), PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString());
        if (sagas.size() == 0) {
          this.startArchiveAndReturnSaga(penRequestBatchEntity);
        } else {
          log.warn("Archive and return saga has already started :: for pen request batch :: {} ", penRequestBatchEntity.getPenRequestBatchID());
        }
      } else {
        this.updatePenRequestBatchStatus(penReqBatchEntities, penRequestBatchEntity, redisKey, PenRequestBatchStatusCodes.ACTIVE.getCode());
      }
    }
  }

  private void handleAllDuplicateOrRepeat(final List<PenRequestBatchEntity> penReqBatchEntities, final PenRequestBatchEntity penRequestBatchEntity, final String redisKey) {
    // duplicates, need to be marked active.
    this.setDifferentCounts(penRequestBatchEntity);
    this.updatePenRequestBatchStatus(penReqBatchEntities, penRequestBatchEntity, redisKey, PenRequestBatchStatusCodes.ACTIVE.getCode());
  }

  private void updatePenRequestBatchStatus(final List<PenRequestBatchEntity> penReqBatchEntities, final PenRequestBatchEntity penRequestBatchEntity, final String redisKey, final String statusCode) {
    penRequestBatchEntity.setPenRequestBatchStatusCode(statusCode);
    final PenRequestBatchHistoryEntity penRequestBatchHistory = historyMapper.toModelFromBatch(penRequestBatchEntity, PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
    penRequestBatchEntity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
    penReqBatchEntities.add(penRequestBatchEntity);
    // put expiring key and value for 5 minutes
    this.stringRedisTemplate.opsForValue().set(redisKey, "true", 5, TimeUnit.MINUTES);
  }

  private void startArchiveAndReturnSaga(final PenRequestBatchEntity penRequestBatchEntity) {
    final var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
      .schoolName(penRequestBatchEntity.getSchoolName())
      .createUser(penRequestBatchEntity.getCreateUser())
      .build();

    final var orchestrator = this.getSagaOrchestrators().get(PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString());
    try {
      final var saga = orchestrator.createSaga(JsonUtil.getJsonStringFromObject(sagaData),
        null, penRequestBatchEntity.getPenRequestBatchID(), sagaData.getCreateUser());
      orchestrator.startSaga(saga);
    } catch (final JsonProcessingException e) {
      log.error("JsonProcessingException while startArchiveAndReturnSaga", e);
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  /**
   * Sets different counts.
   *
   * @param penRequestBatchEntity the pen request batch entity
   */
  private void setDifferentCounts(final PenRequestBatchEntity penRequestBatchEntity) {
    penRequestBatchEntity.setErrorCount(this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(PenRequestBatchStudentStatusCodes.ERROR.getCode())));
    penRequestBatchEntity.setFixableCount(this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(PenRequestBatchStudentStatusCodes.FIXABLE.getCode())));
    penRequestBatchEntity.setMatchedCount(this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode())));
    penRequestBatchEntity.setNewPenCount(this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode())));
    penRequestBatchEntity.setDuplicateCount(this.penRequestBatchStudentRepository.countAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIn(penRequestBatchEntity, List.of(DUPLICATE.getCode())));
  }

  /**
   * no need of REDIS here as sagas are idempotent and they have there own checks.
   * Find and process uncompleted sagas.
   */
  @Async("taskExecutor")
  @Transactional
  public void findAndProcessUncompletedSagas() {
    final var sagas = this.getSagaRepository().findAllByStatusIn(this.getStatusFilters());
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        if (saga.getCreateDate().isBefore(LocalDateTime.now().minusMinutes(5))
          && this.getSagaOrchestrators().containsKey(saga.getSagaName())) {
          try {
            this.setRetryCountAndLog(saga);
            this.getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
          } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
          } catch (final IOException | TimeoutException e) {
            log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
          }
        }
      }
    }
  }

  /**
   * Publish unprocessed student records.
   */
  @Async("taskExecutor")
  @Transactional
  public void publishRepeatCheckedStudentsForFurtherProcessing() {
    long pendingSagaCount = this.sagaRepository.countAllByStatusIn(this.getStatusFilters());
    if (pendingSagaCount > 100) {
      log.info("Pending saga count is {}. No need to publish unprocessed student records.", pendingSagaCount);
      return;
    }
    final Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = this.findRepeatsCheckedStudentRecordsToBeProcessed();
    log.debug("found :: {}  records to be processed", penRequestBatchStudents.size());
    if (!penRequestBatchStudents.isEmpty()) {
      this.getPenRegBatchStudentRecordsProcessor().publishUnprocessedStudentRecordsForProcessing(penRequestBatchStudents);
    }
  }

  /**
   * Find repeats checked student records to be processed set.
   *
   * @return the set
   */
  private Set<PenRequestBatchStudentSagaData> findRepeatsCheckedStudentRecordsToBeProcessed() {
    val penRequestBatchStudents = new HashSet<PenRequestBatchStudentSagaData>();
    val penReqBatches = this.getPenRequestBatchRepository().findByPenRequestBatchStatusCode(REPEATS_CHECKED.getCode());
    for (val penRequestBatch : penReqBatches) {
      val inProgressStudentIDList =
        this.getSagaRepository().findAllPenRequestBatchStudentIDByPenRequestBatchIDAndSagaName(penRequestBatch.getPenRequestBatchID(),
          PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString()).stream().map(PenRequestBatchStudentIDProjection::getPenRequestBatchStudentID).collect(Collectors.toSet());
      val prbStudents = this.getPenRequestBatchStudentRepository().findTop100ByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeInOrderByCreateDate(penRequestBatch, List.of(LOADED.getCode()));
      for (val penReqBatchStudent : prbStudents) {
        if (inProgressStudentIDList.contains(penReqBatchStudent.getPenRequestBatchStudentID())) {
          continue;
        }
        penRequestBatchStudents.add(PenRegBatchHelper.createSagaDataFromStudentRequestAndBatch(penReqBatchStudent, penRequestBatch));
      }
    }
    return penRequestBatchStudents;
  }

  /**
   * * This is EITHER for the edge case scenarios when the pod which was processing the batch file dies before persisting
   * * the repeat check updates.
   * * OR when the file was held for certain condition and it was released by pen coordinator for further processing.
   */
  @Async("taskExecutor")
  @Transactional
  public void checkLoadedStudentRecordsForDuplicatesAndRepeatsAndPublishForFurtherProcessing() {
    final LocalDateTime createDateToCompare = LocalDateTime.now().minusMinutes(10);
    final var penReqBatches = this.getPenRequestBatchRepository().findByPenRequestBatchStatusCodeAndCreateDateBefore(
      LOADED.getCode(), createDateToCompare);
    log.debug("found :: {}  records to be checked for repeats", penReqBatches.size());
    if (!penReqBatches.isEmpty()) {
      this.getPenRegBatchStudentRecordsProcessor().checkLoadedStudentRecordsForDuplicatesAndRepeatsAndPublishForFurtherProcessing(penReqBatches);
    }
  }

  /**
   * Gets status filters.
   *
   * @return the status filters
   */
  public List<String> getStatusFilters() {
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }

  private void setRetryCountAndLog(final Saga saga) {
    Integer retryCount = saga.getRetryCount();
    if (retryCount == null || retryCount == 0) {
      retryCount = 1;
    } else {
      retryCount += 1;
    }
    saga.setRetryCount(retryCount);
    this.getSagaRepository().save(saga);
    LogHelper.logSagaRetry(saga);
  }
}
