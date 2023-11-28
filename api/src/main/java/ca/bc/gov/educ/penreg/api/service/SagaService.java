package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchMultiplePen;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnAllSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.STARTED;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Saga service.
 */
@Service
@Slf4j
public class SagaService {
  /**
   * The Saga repository.
   */
  @Getter(AccessLevel.PRIVATE)
  private final SagaRepository sagaRepository;
  /**
   * The Saga event repository.
   */
  @Getter(PRIVATE)
  private final SagaEventRepository sagaEventRepository;
  /**
   * The Pen request batch student repository.
   */
  private final PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  /**
   * Instantiates a new Saga service.
   *
   * @param sagaRepository                   the saga repository
   * @param sagaEventRepository              the saga event repository
   * @param penRequestBatchStudentRepository the pen request batch student repository
   */
  @Autowired
  public SagaService(final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository, final PenRequestBatchStudentRepository penRequestBatchStudentRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
  }


  /**
   * Create saga record saga.
   *
   * @param saga the saga
   * @return the saga
   */
  public Saga createSagaRecord(final Saga saga) {
    return this.getSagaRepository().save(saga);
  }

  /**
   * no need to do a get here as it is an attached entity
   * first find the child record, if exist do not add. this scenario may occur in replay process,
   * so dont remove this check. removing this check will lead to duplicate records in the child table.
   *
   * @param saga      the saga object.
   * @param sagaEvent the saga event
   */
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateAttachedSagaWithEvents(final Saga saga, final SagaEvent sagaEvent) {
    try {
      saga.setUpdateDate(LocalDateTime.now());
      this.getSagaRepository().save(saga);
      val result = this.getSagaEventRepository()
          .findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEvent.getSagaEventOutcome(), sagaEvent.getSagaEventState(), sagaEvent.getSagaStepNumber() - 1); //check if the previous step was same and had same outcome, and it is due to replay.
      if (result.isEmpty()) {
        this.getSagaEventRepository().save(sagaEvent);
      }
    } catch (Exception e) {
      log.error("updateAttachedSagaWithEvents failed for PenRequestBatchId :: {}, SagaId :: {}, SagaEventState :: {}, Error :: {}", saga.getPenRequestBatchID(), saga.getSagaId(), sagaEvent.getSagaEventState(), e.toString());
      throw new PenRegAPIRuntimeException(e);
    }
  }

  /**
   * Find saga by id optional.
   *
   * @param sagaId the saga id
   * @return the optional
   */
  public Optional<Saga> findSagaById(final UUID sagaId) {
    return this.getSagaRepository().findById(sagaId);
  }

  /**
   * Find all saga states list.
   *
   * @param saga the saga
   * @return the list
   */
  public List<SagaEvent> findAllSagaStates(final Saga saga) {
    return this.getSagaEventRepository().findBySaga(saga);
  }


  /**
   * Update saga record.
   *
   * @param saga the saga
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void updateSagaRecord(final Saga saga) { // saga here MUST be an attached entity
    this.getSagaRepository().save(saga);
  }

  /**
   * Find by pen request batch student id optional.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @param sagaName                 the saga name
   * @return the list
   */
  public List<Saga> findByPenRequestBatchStudentIDAndSagaName(final UUID penRequestBatchStudentID, final String sagaName) {
    return this.getSagaRepository().findByPenRequestBatchStudentIDAndSagaName(penRequestBatchStudentID, sagaName);
  }

  /**
   * Find all by pen request batch student id and status in list.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @param statuses                 the statuses
   * @return the list
   */
  public List<Saga> findAllByPenRequestBatchStudentIDAndStatusIn(final UUID penRequestBatchStudentID, final List<String> statuses) {
    return this.getSagaRepository().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID, statuses);
  }

  /**
   * Find all by pen request batch id in and status in list.
   *
   * @param penRequestBatchIDs the pen request batch i ds
   * @param statuses           the statuses
   * @return the list
   */
  public List<Saga> findAllByPenRequestBatchIDInAndStatusIn(final List<UUID> penRequestBatchIDs, final List<String> statuses) {
    return this.getSagaRepository().findAllByPenRequestBatchIDInAndStatusIn(penRequestBatchIDs, statuses);
  }

  /**
   * Update attached entity during saga process.
   *
   * @param saga the saga
   */
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateAttachedEntityDuringSagaProcess(final Saga saga) {
    this.getSagaRepository().save(saga);
  }


  /**
   * Create saga record in db saga.
   *
   * @param sagaName                 the saga name
   * @param userName                 the user name
   * @param payload                  the payload
   * @param penRequestBatchStudentID the pen request batch student id
   * @param penRequestBatchID        the pen request batch id
   * @return the saga
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Saga createSagaRecordInDB(final String sagaName, final String userName, final String payload, final UUID penRequestBatchStudentID, final UUID penRequestBatchID) {
    final var saga = Saga
      .builder()
      .payload(payload)
      .penRequestBatchStudentID(penRequestBatchStudentID)
      .penRequestBatchID(penRequestBatchID)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .createDate(LocalDateTime.now())
      .createUser(userName)
      .updateUser(userName)
      .updateDate(LocalDateTime.now())
      .build();
    return this.createSagaRecord(saga);
  }


  /**
   * Create saga record in db saga.
   *
   * @param sagaName the saga name
   * @param userName the user name
   * @param payloads the list of pen request batch id and the payload
   * @return the saga
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<Saga> createMultipleBatchSagaRecordsInDB(final String sagaName, final String userName, final List<Pair<UUID, String>> payloads) {
    final List<Saga> sagas = new ArrayList<>();
    payloads.forEach(payloadPair -> sagas.add(
      Saga.builder()
        .payload(payloadPair.getSecond())
        .penRequestBatchID(payloadPair.getFirst())
        .sagaName(sagaName)
        .status(STARTED.toString())
        .sagaState(INITIATED.toString())
        .createDate(LocalDateTime.now())
        .createUser(userName)
        .updateUser(userName)
        .updateDate(LocalDateTime.now())
        .build()));

    return this.getSagaRepository().saveAll(sagas);
  }

  /**
   * Find all completable future.
   *
   * @param specs      the saga specs
   * @param pageNumber the page number
   * @param pageSize   the page size
   * @param sorts      the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<Saga>> findAll(final Specification<Saga> specs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.sagaRepository.findAll(specs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }

  /**
   * Find duplicate pen assigned to diff pen request in same batch list.
   *
   * @param penRequestBatchArchiveAndReturnAllSagaData the pen request batch archive and return all saga data
   * @return the list ArchiveAndReturnSagaResponse with error message.
   */
  public Optional<String> findDuplicatePenAssignedToDiffPenRequestInSameBatch(final PenRequestBatchArchiveAndReturnAllSagaData penRequestBatchArchiveAndReturnAllSagaData) {
    val penRequestBatchIDs = penRequestBatchArchiveAndReturnAllSagaData.getPenRequestBatchArchiveAndReturnSagaData()
      .stream().map(PenRequestBatchArchiveAndReturnSagaData::getPenRequestBatchID).collect(Collectors.toList());
    return this.findDuplicatePenAssignedToDiffPRInSameBatchByBatchIds(penRequestBatchIDs);
  }

  public Optional<String> findDuplicatePenAssignedToDiffPRInSameBatchByBatchIds(final List<UUID> penRequestBatchIDs) {
    final List<PenRequestBatchMultiplePen> recordWithMultiples = this.penRequestBatchStudentRepository.findBatchFilesWithMultipleAssignedPens(penRequestBatchIDs);
    if (!recordWithMultiples.isEmpty()) {
      final List<String> errorResponse = new ArrayList<>();
      recordWithMultiples.forEach(el -> errorResponse.add("Unable to archive submission number# " + el.getSubmissionNumber() + " due to multiple records assigned the same PEN."));
      return Optional.of(String.join(",", errorResponse));
    }
    return Optional.empty();
  }
  public Optional<SagaEvent> findSagaEvent(final Saga saga, final SagaEvent sagaEvent) {
    return this.sagaEventRepository.findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEvent.getSagaEventOutcome(), sagaEvent.getSagaEventState(), sagaEvent.getSagaStepNumber());
  }

  public Optional<SagaEvent> findSagaEvent(final Saga saga, final String eventState, int stepNumber) {
    return this.sagaEventRepository.findBySagaAndSagaEventStateAndSagaStepNumber(saga, eventState, stepNumber);
  }
}
