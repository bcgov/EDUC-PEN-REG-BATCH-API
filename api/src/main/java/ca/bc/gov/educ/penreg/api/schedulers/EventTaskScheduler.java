package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchStudentRecordsProcessor;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper.PEN_REQUEST_BATCH_API;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class EventTaskScheduler implements Closeable {
  /**
   * The Executor service.
   */
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  /**
   * The Saga orchestrators.
   */
  @Getter(PRIVATE)
  private final Map<String, BaseOrchestrator<?>> sagaOrchestrators = new HashMap<>();
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
   * The constant mapper.
   */
  private static final PenRequestBatchStudentSagaDataMapper mapper = PenRequestBatchStudentSagaDataMapper.mapper;

  /**
   * The Status filters.
   */
  @Setter
  private List<String> statusFilters;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param sagaRepository                     the saga repository
   * @param penRequestBatchRepository          the pen request batch repository
   * @param penRequestBatchStudentRepository   the pen request batch student repository
   * @param penRegBatchStudentRecordsProcessor the pen reg batch student records processor
   */
  @Autowired
  public EventTaskScheduler(SagaRepository sagaRepository, PenRequestBatchRepository penRequestBatchRepository, PenRequestBatchStudentRepository penRequestBatchStudentRepository, PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor) {
    this.sagaRepository = sagaRepository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
  }

  /**
   * Register saga orchestrators.
   *
   * @param sagaName     the saga name
   * @param orchestrator the orchestrator
   */
  public void registerSagaOrchestrators(String sagaName, BaseOrchestrator<?> orchestrator) {
    getSagaOrchestrators().put(sagaName, orchestrator);
  }

  /**
   * Run the job every minute to check how many records are in IN_PROGRESS or STARTED status and has not been updated in last 5 minutes.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.uncompleted.sagas.cron}") // 1 * * * * *
  @SchedulerLock(name = "EXTRACT_UNCOMPLETED_SAGAS",
      lockAtLeastFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtMostFor}")
  @Transactional
  public void findAndProcessPendingSagaEvents() {
    LockAssert.assertLocked();
    executorService.execute(() -> {
      var sagas = getSagaRepository().findAllByStatusIn(getStatusFilters());
      if (!sagas.isEmpty()) {
        for (val saga : sagas) {
          if (saga.getCreateDate().isBefore(LocalDateTime.now().minusMinutes(5))
              && getSagaOrchestrators().containsKey(saga.getSagaName())) {
            try {
              getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
            } catch (IOException | InterruptedException | TimeoutException e) {
              log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
            }
          }
        }
      }
    });
  }

  /**
   * Process loaded pen request batches.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.unprocessed.students.cron}")
  @SchedulerLock(name = "EXTRACT_UNPROCESSED_STUDENT_RECORDS",
      lockAtLeastFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtMostFor}")
  @Transactional
  public void processLoadedPenRequestBatches() {
    LockAssert.assertLocked();
    executorService.execute(() -> {
      log.info("started processLoadedPenRequestBatches");
      Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = findLoadedStudentRecordsToBeProcessed();
      log.info("found :: {}  records to be processed", penRequestBatchStudents.size());
      if (!penRequestBatchStudents.isEmpty()) {
        getPenRegBatchStudentRecordsProcessor().publishUnprocessedStudentRecordsForProcessing(penRequestBatchStudents);
      }
    });
  }

  /**
   * Find all the pen request batch that has been processed and update their status and add history record.
   */
  @Scheduled(cron = "1 * * * * *") // every 1 minutes
  @SchedulerLock(name = "MARK_PROCESSED_BATCHES_ACTIVE",
      lockAtLeastFor = "50s", lockAtMostFor = "52s")
  @Transactional
  public void markProcessedPenRequestBatchesActive() {
    LockAssert.assertLocked();
    executorService.execute(() -> {
      var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(LOADED.getCode());
      if (!penReqBatches.isEmpty()) {
        var penReqBatchEntities = new ArrayList<PenRequestBatchEntity>();
        penReqBatches.forEach(el -> {
          var students = getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsNot(el, LOADED.getCode());
          if (students.isEmpty()) { // all records have been processed for this batch, make it active.
            el.setPenRequestBatchStatusCode(ACTIVE.getCode());
            PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(el, ACTIVE.getCode(), STATUS_CHANGED.getCode(), null);
            el.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
            penReqBatchEntities.add(el);
          }
        });
        if (!penReqBatchEntities.isEmpty()) {
          getPenRequestBatchRepository().saveAll(penReqBatchEntities); // update all of them in one commit.
        }
      }
    });
  }


  /**
   * Create pen req batch history pen request batch history entity.
   *
   * @param entity     the entity
   * @param statusCode the status code
   * @param eventCode  the event code
   * @param reason     the reason
   * @return the pen request batch history entity
   */
  private PenRequestBatchHistoryEntity createPenReqBatchHistory(@NonNull PenRequestBatchEntity entity, String statusCode, String eventCode, String reason) {
    var penRequestBatchHistory = new PenRequestBatchHistoryEntity();
    penRequestBatchHistory.setCreateDate(LocalDateTime.now());
    penRequestBatchHistory.setUpdateDate(LocalDateTime.now());
    penRequestBatchHistory.setPenRequestBatchEntity(entity);
    penRequestBatchHistory.setPenRequestBatchStatusCode(statusCode);
    penRequestBatchHistory.setPenRequestBatchEventCode(eventCode);
    penRequestBatchHistory.setCreateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setUpdateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setEventDate(LocalDateTime.now());
    penRequestBatchHistory.setEventReason(reason);
    return penRequestBatchHistory;
  }

  /**
   * Find loaded student records to be processed set.
   *
   * @return the set
   */
  private Set<PenRequestBatchStudentSagaData> findLoadedStudentRecordsToBeProcessed() {
    Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = new HashSet<>();
    var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(LOADED.getCode());
    penReqBatches.forEach(penRequestBatchEntity -> {
      val studentEntitiesAlreadyInProcess = getSagaRepository().findByPenRequestBatchID(penRequestBatchEntity.getPenRequestBatchID());
      penRequestBatchStudents.addAll(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().filter(penRequestBatchStudentEntity ->
          studentEntitiesAlreadyInProcess.stream()
              .allMatch(saga -> (!penRequestBatchStudentEntity.getPenRequestBatchStudentID().equals(saga.getPenRequestBatchStudentID()))))
          .map(mapper::toPenReqBatchStudentSagaData)
          .peek(penRequestBatchStudentSagaData -> penRequestBatchStudentSagaData.setMincode(penRequestBatchEntity.getMinCode()))
          .collect(Collectors.toSet()));
    });
    return penRequestBatchStudents;
  }

  /**
   * Gets status filters.
   *
   * @return the status filters
   */
  public List<String> getStatusFilters() {
    if (statusFilters != null && !statusFilters.isEmpty()) {
      return statusFilters;
    } else {
      var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }

  }

  /**
   * Close.
   */
  @Override
  public void close() {
    if (!executorService.isShutdown()) {
      executorService.shutdown();
    }
  }
}
