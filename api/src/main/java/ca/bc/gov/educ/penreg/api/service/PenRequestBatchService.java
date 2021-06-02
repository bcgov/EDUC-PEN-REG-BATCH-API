package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.mappers.PenStudentDemogValidationMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.interfaces.PenMatchResultProcessingService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.VALIDATION_SUCCESS_WITH_ERROR;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REARCHIVED;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen reg batch service.
 *
 * @author OM
 */
@Service
@Slf4j
public class PenRequestBatchService {

  /**
   * The constant historyMapper.
   */
  private static final PenRequestBatchHistoryMapper historyMapper = PenRequestBatchHistoryMapper.mapper;
  /**
   * The Repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchRepository repository;
  /**
   * The Pen web blob repository.
   */
  @Getter(PRIVATE)
  private final PenWebBlobRepository penWebBlobRepository;

  /**
   * the pen request batch student repository
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  /**
   * the pen response file generator service
   */
  @Getter(PRIVATE)
  private final ResponseFileGeneratorService responseFileGeneratorService;

  /**
   * The Rest utils.
   */
  @Getter(PRIVATE)
  private final RestUtils restUtils;

  /**
   * The Processing service.
   */
  private final PenMatchResultProcessingService<PenRequestPenMatchProcessingPayload, org.apache.commons.lang3.tuple.Pair<Integer, Optional<PenRequestResult>>> processingService;

  /**
   * Instantiates a new Pen reg batch service.
   *
   * @param repository                       the repository
   * @param penWebBlobRepository             the pen web blob repository
   * @param penRequestBatchStudentRepository the pen request batch student repository
   * @param responseFileGeneratorService     the response file generator service
   * @param restUtils                        the rest utils
   * @param processingService                the processing service
   */
  @Autowired
  public PenRequestBatchService(final PenRequestBatchRepository repository,
                                final PenWebBlobRepository penWebBlobRepository,
                                final PenRequestBatchStudentRepository penRequestBatchStudentRepository,
                                final ResponseFileGeneratorService responseFileGeneratorService,
                                final RestUtils restUtils,
                                @Qualifier("penRequestPenMatchResultProcessingService") final PenMatchResultProcessingService<PenRequestPenMatchProcessingPayload,
                                org.apache.commons.lang3.tuple.Pair<Integer, Optional<PenRequestResult>>> processingService) {
    this.repository = repository;
    this.penWebBlobRepository = penWebBlobRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.responseFileGeneratorService = responseFileGeneratorService;
    this.restUtils = restUtils;
    this.processingService = processingService;
  }

  /**
   * Find all completable future.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param pageNumber       the page number
   * @param pageSize         the page size
   * @param sorts            the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<PenRequestBatchEntity>> findAll(final Specification<PenRequestBatchEntity> penRegBatchSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.getRepository().findAll(penRegBatchSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }

  /**
   * Find all by PenRequestBatchStudent completable future.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param pageNumber       the page number
   * @param pageSize         the page size
   * @param sorts            the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<Pair<PenRequestBatchEntity, Long>>> findAllByPenRequestBatchStudent(@NonNull final Specification<PenRequestBatchEntity> penRegBatchSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.getRepository().findByPenRequestBatchStudent(penRegBatchSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }

  /**
   * Gets pen request batch entity by id.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch entity by id
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public Optional<PenRequestBatchEntity> getPenRequestBatchEntityByID(final UUID penRequestBatchID) {
    return this.getRepository().findById(penRequestBatchID);
  }

  /**
   * Create pen request batch pen request batch entity.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchEntity savePenRequestBatch(final PenRequestBatchEntity penRequestBatchEntity) {
    final PenRequestBatchHistoryEntity penRequestBatchHistory = historyMapper.toModelFromBatch(penRequestBatchEntity, PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
    penRequestBatchEntity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
    return this.getRepository().save(penRequestBatchEntity);
  }


  /**
   * Update pen request batch pen request batch entity.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @param penRequestBatchID     the pen request batch id
   * @return the pen request batch entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchEntity updatePenRequestBatch(final PenRequestBatchEntity penRequestBatchEntity, final UUID penRequestBatchID) {
    final var penRequestBatchEntityOptional = this.getRepository().findById(penRequestBatchID);
    return penRequestBatchEntityOptional.map(penRequestBatchEntityDB -> {
      this.checkAndPopulateStatusCode(penRequestBatchEntity, penRequestBatchEntityDB);
      BeanUtils.copyProperties(penRequestBatchEntity, penRequestBatchEntityDB,
        "penRequestBatchStudentEntities", "penRequestBatchHistoryEntities", "createUser", "createDate");
      penRequestBatchEntityDB.setPenRequestBatchID(penRequestBatchID);
      final PenRequestBatchHistoryEntity penRequestBatchHistory = historyMapper.toModelFromBatch(penRequestBatchEntity, PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
      penRequestBatchEntityDB.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
      return this.getRepository().save(penRequestBatchEntityDB);
    }).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Find pen request batch by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchEntity> findPenRequestBatchBySubmissionNumber(@NonNull final String submissionNumber) {
    return this.getRepository().findBySubmissionNumber(submissionNumber);
  }

  /**
   * Find pen web blobs by submission number and file type.
   *
   * @param submissionNumber the submission number
   * @param fileType         the file type
   * @return the list
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PENWebBlobEntity> findPenWebBlobBySubmissionNumberAndFileType(@NonNull final String submissionNumber, @NonNull final String fileType) {
    return this.getPenWebBlobRepository().findAllBySubmissionNumberAndFileType(submissionNumber, fileType);
  }

  /**
   * Find pen web blobs by submission number.
   *
   * @param submissionNumber the submission number
   * @return the list
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PENWebBlobEntity> findPenWebBlobBySubmissionNumber(@NonNull final String submissionNumber) {
    return this.getPenWebBlobRepository().findAllBySubmissionNumber(submissionNumber);
  }

  /**
   * Update pen web blob pen web blob entity.
   *
   * @param entity       the entity
   * @param penwebBlobID the penweb blob id
   * @return the pen web blob entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PENWebBlobEntity updatePenWebBlob(@NonNull final PENWebBlobEntity entity, final Long penwebBlobID) {
    final var penWebBlobEntity = this.getPenWebBlobRepository().findById(penwebBlobID).map(dbEntity -> {
      dbEntity.setExtractDateTime(entity.getExtractDateTime());
      return dbEntity;
    }).orElseThrow(EntityNotFoundException::new);
    return this.getPenWebBlobRepository().save(penWebBlobEntity);
  }

  /**
   * Delete pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void deletePenRequestBatch(final UUID penRequestBatchID) {
    final var penRequestBatchEntity = this.getRepository().findById(penRequestBatchID).orElseThrow(EntityNotFoundException::new);
    this.getRepository().delete(penRequestBatchEntity);
  }

  /**
   * Find by id optional.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the optional
   */
  public Optional<PenRequestBatchEntity> findById(final UUID penRequestBatchID) {
    return this.repository.findById(penRequestBatchID);
  }

  public List<PenRequestIDs> findAllPenRequestIDs(final List<UUID> penRequestBatchIDs, final List<PenRequestBatchStudentStatusCodes> penRequestBatchStudentStatusCodes) {
    return this.penRequestBatchStudentRepository.getAllPenRequestBatchStudentIDs(penRequestBatchIDs,
      penRequestBatchStudentStatusCodes.stream().map(PenRequestBatchStudentStatusCodes::getCode).collect(Collectors.toList()));
  }

  /**
   * Gets stats.
   *
   * @return the stats
   */
  @Transactional(propagation = Propagation.SUPPORTS)
  public PenRequestBatchStats getStats() {
    final List<PenRequestBatchStat> penRequestBatchStats = new ArrayList<>();
    for (final SchoolGroupCodes schoolGroupCode : SchoolGroupCodes.values()) {
      val results = this.getRepository().findByPenRequestBatchStatusCodeAndSchoolGroupCode(PenRequestBatchStatusCodes.ACTIVE.getCode(), schoolGroupCode.getCode());
      final PenRequestBatchStat stats = this.calculateFixableAndRepeats(results);
      stats.setSchoolGroupCode(schoolGroupCode.getCode());
      List<String> penReqBatchStatusCodes = Arrays.asList(PenRequestBatchStatusCodes.UNARCHIVED.getCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode());
      stats.setUnarchivedCount(this.getRepository().countAllByPenRequestBatchStatusCodeInAndSchoolGroupCode(penReqBatchStatusCodes, schoolGroupCode.getCode()));
      if (schoolGroupCode == SchoolGroupCodes.PSI) {
        penReqBatchStatusCodes = Arrays.asList(PenRequestBatchStatusCodes.HOLD_SIZE.getCode(), PenRequestBatchStatusCodes.DUPLICATE.getCode());
        stats.setHeldForReviewCount(this.getRepository().countAllByPenRequestBatchStatusCodeInAndSchoolGroupCode(penReqBatchStatusCodes, schoolGroupCode.getCode()));
      } else {
        stats.setHeldForReviewCount(0L);
      }
      penRequestBatchStats.add(stats);
    }
    // this is irrespective of school group code.
    val loadFailedCount = this.getRepository().countPenRequestBatchEntitiesByPenRequestBatchStatusCode(PenRequestBatchStatusCodes.LOAD_FAIL.getCode());
    return PenRequestBatchStats.builder().penRequestBatchStatList(penRequestBatchStats).loadFailCount(loadFailedCount).build();
  }

  /**
   * Calculate fixable and repeats pen request batch stat.
   *
   * @param results the results
   * @return the pen request batch stat
   */
  private PenRequestBatchStat calculateFixableAndRepeats(final List<PenRequestBatchEntity> results) {
    final PenRequestBatchStat.PenRequestBatchStatBuilder builder = PenRequestBatchStat.builder();
    long fixableCount = 0;
    long repeatCount = 0;
    for (val result : results) {
      if (result != null) {
        if (result.getFixableCount() != null) {
          fixableCount += result.getFixableCount();
        }
        if (result.getRepeatCount() != null) {
          repeatCount += result.getRepeatCount();
        }
      }
    }
    builder.fixableCount(fixableCount);
    builder.repeatCount(repeatCount);
    builder.pendingCount((long) results.size());
    return builder.build();
  }

  /**
   * Check and populate status code.
   *
   * @param requestPrbEntity the request prb entity
   * @param currentPrbEntity the current prb entity
   */
  private void checkAndPopulateStatusCode(final PenRequestBatchEntity requestPrbEntity, final PenRequestBatchEntity currentPrbEntity) {
    if (StringUtils.equals(requestPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.ARCHIVED.getCode())
      && (StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())
      || StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode()))) {
      requestPrbEntity.setPenRequestBatchStatusCode(REARCHIVED.getCode());
    }
  }

  /**
   * Populate student data from batch map.
   *
   * @param penRequestBatch the pen request batch
   * @return the map
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  public Map<String, Student> populateStudentDataFromBatch(final PenRequestBatchEntity penRequestBatch) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    final List<UUID> studentIDs = penRequestBatch.getPenRequestBatchStudentEntities().stream()
      .map(PenRequestBatchStudentEntity::getStudentID).filter(Objects::nonNull).collect(Collectors.toList());
    if (!studentIDs.isEmpty()) {
      val result = this.restUtils.getStudentsByStudentIDs(studentIDs);
      if (result.isEmpty()) {
        throw new PenRegAPIRuntimeException("got blank response from student api which is not expected :: " + penRequestBatch.getPenRequestBatchID());
      }
      return result.stream().collect(Collectors.toConcurrentMap(Student::getStudentID, Function.identity()));
    }
    return Collections.emptyMap();
  }

  /**
   * Post pen request org . apache . commons . lang 3 . tuple . pair.
   *
   * @param penRequest the pen request
   * @return the org . apache . commons . lang 3 . tuple . pair
   */
  @SneakyThrows(JsonProcessingException.class)
  public org.apache.commons.lang3.tuple.Pair<Integer, Optional<PenRequestResult>> postPenRequest(final PenRequest penRequest) {
    val transactionID = UUID.randomUUID();
    val penValidationPayload = PenStudentDemogValidationMapper.mapper.toValidationPayload(penRequest);
    penValidationPayload.setTransactionID(transactionID.toString());
    val penRequestResult = PenRequestBatchMapper.mapper.toPenRequestResult(penRequest);
    val validationEvent = Event.builder().eventPayload(JsonUtil.getJsonStringFromObject(penValidationPayload)).eventType(EventType.VALIDATE_STUDENT_DEMOGRAPHICS).build();
    val validationResponseEvent = this.restUtils.requestEventResponseFromServicesAPI(validationEvent);
    if (validationResponseEvent.isEmpty()) {
      return org.apache.commons.lang3.tuple.Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    // if the validation rule failed return
    if (validationResponseEvent.get().getEventOutcome() == VALIDATION_SUCCESS_WITH_ERROR) {
      final TypeReference<List<PenRequestBatchStudentValidationIssue>> responseType = new TypeReference<>() {
      };
      val validationResults = new ObjectMapper().readValue(validationResponseEvent.get().getEventPayload(), responseType);
      penRequestResult.setValidationIssues(validationResults.stream().filter(validationResult -> "ERROR".equals(validationResult.getPenRequestBatchValidationIssueSeverityCode())).collect(Collectors.toList()));
      return org.apache.commons.lang3.tuple.Pair.of(HttpStatus.OK.value(), Optional.of(penRequestResult));
    }
    val penMatchPayload = JsonUtil.getJsonStringFromObject(PenRequestBatchMapper.mapper.toPenMatch(penRequest));
    val penMatchEvent = Event.builder().sagaId(transactionID).eventType(EventType.PROCESS_PEN_MATCH).eventPayload(penMatchPayload).build();
    val penMatchResponseEvent = this.restUtils.requestEventResponseFromMatchAPI(penMatchEvent);
    if (penMatchResponseEvent.isEmpty() || StringUtils.isBlank(penMatchResponseEvent.get().getEventPayload())) {
      return org.apache.commons.lang3.tuple.Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, penMatchResponseEvent.get().getEventPayload());
    return this.processingService.processPenMatchResults(PenRequestPenMatchProcessingPayload.builder().transactionID(transactionID).penRequest(penRequest).penRequestResult(penRequestResult).penMatchResult(penMatchResult).build());
  }
}
