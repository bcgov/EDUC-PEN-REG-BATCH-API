package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchStudentRecordsProcessor;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper.PEN_REQUEST_BATCH_API;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@SuppressWarnings("java:S2142")
public class EventTaskSchedulerAsyncService {

  /**
   * The Saga repository.
   */
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

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

  private static final PenRequestBatchStudentSagaDataMapper mapper = PenRequestBatchStudentSagaDataMapper.mapper;

  /**
   * The Status filters.
   */
  @Setter
  private List<String> statusFilters;

  public EventTaskSchedulerAsyncService(SagaRepository sagaRepository, PenRequestBatchRepository penRequestBatchRepository, PenRequestBatchStudentRepository penRequestBatchStudentRepository, PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor) {
    this.sagaRepository = sagaRepository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
  }
  @Async("taskExecutor")
  @Transactional
  public void markProcessedBatchesActive(){
    var penReqBatches = getPenRequestBatchRepository().findByPenRequestBatchStatusCode(LOADED.getCode());
    if (!penReqBatches.isEmpty()) {
      var penReqBatchEntities = new ArrayList<PenRequestBatchEntity>();
      penReqBatches.forEach(el -> {
        var students = getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsNot(el, PenRequestBatchStatusCodes.LOADED.getCode());
        if (students.isEmpty()) { // all records have been processed for this batch, make it active.
          el.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ACTIVE.getCode());
          PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(el, PenRequestBatchStatusCodes.ACTIVE.getCode(), PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
          el.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
          penReqBatchEntities.add(el);
        }
      });
      if (!penReqBatchEntities.isEmpty()) {
        getPenRequestBatchRepository().saveAll(penReqBatchEntities); // update all of them in one commit.
      }
    }
  }

  @Async("taskExecutor")
  @Transactional
  public void findAndProcessUncompletedSagas(Map<String, BaseOrchestrator<?>> sagaOrchestrators){
    var sagas = getSagaRepository().findAllByStatusIn(getStatusFilters());
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        if (saga.getCreateDate().isBefore(LocalDateTime.now().minusMinutes(5))
            &&sagaOrchestrators.containsKey(saga.getSagaName())) {
          try {
            sagaOrchestrators.get(saga.getSagaName()).replaySaga(saga);
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

  @Async("taskExecutor")
  @Transactional
  public void publishUnprocessedStudentRecords(){
    Set<PenRequestBatchStudentSagaData> penRequestBatchStudents = findLoadedStudentRecordsToBeProcessed();
    log.info("found :: {}  records to be processed", penRequestBatchStudents.size());
    if (!penRequestBatchStudents.isEmpty()) {
      getPenRegBatchStudentRecordsProcessor().publishUnprocessedStudentRecordsForProcessing(penRequestBatchStudents);
    }
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
}
