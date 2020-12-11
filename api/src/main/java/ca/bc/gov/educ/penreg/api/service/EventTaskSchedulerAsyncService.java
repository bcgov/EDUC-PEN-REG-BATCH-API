package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchStudentRecordsProcessor;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper.PEN_REQUEST_BATCH_API;
import static ca.bc.gov.educ.penreg.api.constants.EventStatus.DB_COMMITTED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REPEATS_CHECKED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler async service.
 */
@Service
@Slf4j
@SuppressWarnings("java:S2142")
public class EventTaskSchedulerAsyncService {

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchStudentSagaDataMapper mapper = PenRequestBatchStudentSagaDataMapper.mapper;
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
   * The Status filters.
   */
  @Setter
  private List<String> statusFilters;
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
   * The event handler service.
   */
  @Getter(PRIVATE)
  private final EventPublisherService eventPublisherService;

  /**
   * Instantiates a new Event task scheduler async service.
   *
   * @param sagaRepository                     the saga repository
   * @param penRequestBatchRepository          the pen request batch repository
   * @param penRequestBatchStudentRepository   the pen request batch student repository
   * @param penRegBatchStudentRecordsProcessor the pen reg batch student records processor
   * @param penRequestBatchEventRepository     the pen request batch event repository
   * @param eventPublisherService              the event publisher service
   * @param orchestrators                      the orchestrators
   *
   */
  public EventTaskSchedulerAsyncService(SagaRepository sagaRepository, PenRequestBatchRepository penRequestBatchRepository,
                                        PenRequestBatchStudentRepository penRequestBatchStudentRepository,
                                        PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor,
                                        PenRequestBatchEventRepository penRequestBatchEventRepository,
                                        EventPublisherService eventPublisherService, List<Orchestrator> orchestrators) {
    this.sagaRepository = sagaRepository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
    this.penRequestBatchEventRepository = penRequestBatchEventRepository;
    this.eventPublisherService = eventPublisherService;
    orchestrators.forEach(orchestrator -> sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  /**
   * Mark processed batches active.
   */
  @Async("taskExecutor")
  @Transactional
  public void markProcessedBatchesActive() {
    var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(REPEATS_CHECKED.getCode());
    log.debug("found {} records in repeat checked state", penReqBatches.size());
    if (!penReqBatches.isEmpty()) {
      var penReqBatchEntities = new ArrayList<PenRequestBatchEntity>();
      for (var penRequestBatchEntity : penReqBatches) {
        var studentSagaRecords = getSagaRepository().findByPenRequestBatchIDAndSagaName(penRequestBatchEntity.getPenRequestBatchID(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString());
        var studentEntities = penRequestBatchEntity.getPenRequestBatchStudentEntities();
        var repeatCount = studentEntities.stream().filter(student -> PenRequestBatchStudentStatusCodes.REPEAT.toString().equals(student.getPenRequestBatchStudentStatusCode())).count();
        if (penRequestBatchEntity.getStudentCount() == repeatCount) { // all records are repeats, need to be marked active.
          penRequestBatchEntity.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ACTIVE.getCode());
          PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(penRequestBatchEntity, PenRequestBatchStatusCodes.ACTIVE.getCode(), PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
          penRequestBatchEntity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
          penReqBatchEntities.add(penRequestBatchEntity);
        } else if (!studentSagaRecords.isEmpty()) {
          long count = studentSagaRecords.stream().filter(saga -> saga.getStatus().equalsIgnoreCase(SagaStatusEnum.COMPLETED.toString())).count();
          if (count == studentSagaRecords.size()) { // All records are processed mark batch to active.
            setDifferentCounts(penRequestBatchEntity, studentEntities);
            penRequestBatchEntity.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ACTIVE.getCode());
            PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(penRequestBatchEntity, PenRequestBatchStatusCodes.ACTIVE.getCode(), PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
            penRequestBatchEntity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
            penReqBatchEntities.add(penRequestBatchEntity);
          }
        }
      }
      if (!penReqBatchEntities.isEmpty()) {
        log.info("marking {} records ACTIVE", penReqBatchEntities.size());
        getPenRequestBatchRepository().saveAll(penReqBatchEntities); // update all of them in one commit.
      }
    }
  }

  private void setDifferentCounts(PenRequestBatchEntity penRequestBatchEntity, Set<PenRequestBatchStudentEntity> studentEntities) {
    //run through the student records and update the counts on the header record...
    long errorCount = 0;
    long fixableCount = 0;
    long matchedCount = 0;
    long newCount = 0;
    for (var studentReq : studentEntities) {
      if (PenRequestBatchStudentStatusCodes.FIXABLE.getCode().equals(studentReq.getPenRequestBatchStudentStatusCode())) {
        fixableCount++;
      }
      if (PenRequestBatchStudentStatusCodes.ERROR.getCode().equals(studentReq.getPenRequestBatchStudentStatusCode())) {
        errorCount++;
      }
      if (PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode().equals(studentReq.getPenRequestBatchStudentStatusCode())) {
        matchedCount++;
      }
      if (PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode().equals(studentReq.getPenRequestBatchStudentStatusCode())) {
        newCount++;
      }
    }
    penRequestBatchEntity.setErrorCount(errorCount);
    penRequestBatchEntity.setFixableCount(fixableCount);
    penRequestBatchEntity.setMatchedCount(matchedCount);
    penRequestBatchEntity.setNewPenCount(newCount);
  }

  /**
   * Find and process uncompleted sagas.
   *
   */
  @Async("taskExecutor")
  @Transactional
  public void findAndProcessUncompletedSagas() {
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
  }

  /**
   * Create pen req batch history pen request batch history entity.
   *
   * @param entity     the entity
   * @param statusCode the status code
   * @param eventCode  the event code
   * @return the pen request batch history entity
   */
  private PenRequestBatchHistoryEntity createPenReqBatchHistory(@NonNull PenRequestBatchEntity entity, String statusCode, String eventCode) {
    var penRequestBatchHistory = new PenRequestBatchHistoryEntity();
    penRequestBatchHistory.setCreateDate(LocalDateTime.now());
    penRequestBatchHistory.setUpdateDate(LocalDateTime.now());
    penRequestBatchHistory.setPenRequestBatchEntity(entity);
    penRequestBatchHistory.setPenRequestBatchStatusCode(statusCode);
    penRequestBatchHistory.setPenRequestBatchEventCode(eventCode);
    penRequestBatchHistory.setCreateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setUpdateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setEventDate(LocalDateTime.now());
    penRequestBatchHistory.setEventReason(null);
    return penRequestBatchHistory;
  }

  /**
   * Publish unprocessed student records.
   */
  @Async("taskExecutor")
  @Transactional
  public void publishUnprocessedStudentRecords() {
    Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = findRepeatsCheckedStudentRecordsToBeProcessed();
    log.debug("found :: {}  records to be processed", penRequestBatchStudents.size());
    if (!penRequestBatchStudents.isEmpty()) {
      getPenRegBatchStudentRecordsProcessor().publishUnprocessedStudentRecordsForProcessing(penRequestBatchStudents);
    }
  }

  /**
   * Find repeats checked student records to be processed set.
   *
   * @return the set
   */
  private Set<PenRequestBatchStudentSagaData> findRepeatsCheckedStudentRecordsToBeProcessed() {
    Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = new HashSet<>();
    var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(REPEATS_CHECKED.getCode());
    penReqBatches.forEach(penRequestBatchEntity -> {
      val studentEntitiesAlreadyInProcess = getSagaRepository().findByPenRequestBatchIDAndSagaName(penRequestBatchEntity.getPenRequestBatchID(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString());
      penRequestBatchStudents.addAll(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().filter(penRequestBatchStudentEntity ->
          studentEntitiesAlreadyInProcess.stream()
              .allMatch(saga -> (!penRequestBatchStudentEntity.getPenRequestBatchStudentID().equals(saga.getPenRequestBatchStudentID()))))
          .map(mapper::toPenReqBatchStudentSagaData)
          .peek(penRequestBatchStudentSagaData -> {
            penRequestBatchStudentSagaData.setMincode(penRequestBatchEntity.getMinCode());
            penRequestBatchStudentSagaData.setPenRequestBatchID(penRequestBatchEntity.getPenRequestBatchID());
          })
          .collect(Collectors.toSet()));
    });
    return penRequestBatchStudents;
  }

  /**
   * Process loaded pen request batches for repeats.
   */
  @Async("taskExecutor")
  @Transactional
  public void processLoadedPenRequestBatchesForRepeats() {
    var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(LOADED.getCode());
    log.debug("found :: {}  records to be checked for repeats", penReqBatches.size());
    if (!penReqBatches.isEmpty()) {
      getPenRegBatchStudentRecordsProcessor().checkLoadedStudentRecordsForRepeats(penReqBatches);
    }
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
   * Poll the event table and publish events
   */
  @Async("taskExecutor")
  public void pollEventTableAndPublish() {
    var events = getPenRequestBatchEventRepository().findByEventStatus(DB_COMMITTED.toString());
    if (!events.isEmpty()) {
      for (PenRequestBatchEvent event : events) {
        try {
          eventPublisherService.send(event);
        } catch (JsonProcessingException e) {
          log.error("Exception while pollEventTableAndPublish :: for event :: {} :: {}", event, e);
        }
      }
    } else {
      log.trace("no unprocessed records.");
    }
  }
}
