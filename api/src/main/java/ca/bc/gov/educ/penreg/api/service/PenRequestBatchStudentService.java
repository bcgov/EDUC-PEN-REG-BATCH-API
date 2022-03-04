package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentStatusCodeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch student service.
 */
@Service
@Slf4j
public class PenRequestBatchStudentService {

  /**
   * The Repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentRepository repository;
  /**
   * The Pen request batch repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchRepository penRequestBatchRepository;

  /**
   * The Pen request batch service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchService penRequestBatchService;

  /**
   * The Student status code repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository;

  /**
   * The Application properties.
   */
  @Getter
  private final ApplicationProperties applicationProperties;

  @Getter
  private final RedissonClient redissonClient;
  @Getter
  private final StringRedisTemplate stringRedisTemplate;

  /**
   * Instantiates a new Pen request batch student service.
   *  @param repository                  the repository
   * @param penRequestBatchRepository   the pen request batch repository
   * @param studentStatusCodeRepository the student status code repository
   * @param applicationProperties       the application properties
   * @param redissonClient the redis client
   * @param stringRedisTemplate the string redis template
   */
  @Autowired
  public PenRequestBatchStudentService(final PenRequestBatchStudentRepository repository, final PenRequestBatchRepository penRequestBatchRepository, final PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository, final PenRequestBatchService penRequestBatchService, final ApplicationProperties applicationProperties, final RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate) {
    this.repository = repository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.studentStatusCodeRepository = studentStatusCodeRepository;
    this.penRequestBatchService = penRequestBatchService;
    this.applicationProperties = applicationProperties;
    this.redissonClient = redissonClient;
    this.stringRedisTemplate = stringRedisTemplate;
  }


  /**
   * Create student pen request batch student entity.
   *
   * @param entity            the entity
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch student entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchStudentEntity createStudent(final PenRequestBatchStudentEntity entity, final UUID penRequestBatchID) {
    final var penRequestBatchOptional = this.getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      entity.setPenRequestBatchEntity(penRequestBatchOptional.get());
      return this.repository.save(entity);
    } else {
      throw new EntityNotFoundException(PenRequestBatchEntity.class, penRequestBatchID.toString());
    }
  }

  /**
   * save student pen request batch student entity. <b> MAKE sure that the parameter is an attached hibernate entity.</b>
   *
   * @param entity the attached entity
   * @return the pen request batch student entity
   */
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PenRequestBatchStudentEntity saveAttachedEntity(final PenRequestBatchStudentEntity entity) {
    entity.setUpdateDate(LocalDateTime.now());
    return this.repository.save(entity);
  }

  /**
   * BE CAREFUL while changing the transaction propagation.
   *
   * @param entity the entity
   */
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveAttachedEntityWithChildEntities(final PenRequestBatchStudentEntity entity) {
    this.repository.save(entity);
  }

  /**
   * Update student pen request batch student entity.
   * EntityNotFoundException will not cause the caller's transaction to rollback
   *
   * @param entity                   the entity
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student entity
   */
  @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = {EntityNotFoundException.class})
  public PenRequestBatchStudentEntity updateStudent(final PenRequestBatchStudentEntity entity, final UUID penRequestBatchID, final UUID penRequestBatchStudentID) {
    final var penRequestBatchOptional = this.getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      final var penRequestBatch = penRequestBatchOptional.get();
      final var penRequestBatchStudentOptional = this.getRepository().findById(penRequestBatchStudentID);
      if (penRequestBatchStudentOptional.isPresent()) {
        final var penRequestBatchStudent = penRequestBatchStudentOptional.get();
        if (penRequestBatchStudent.getPenRequestBatchEntity().getPenRequestBatchID().equals(penRequestBatch.getPenRequestBatchID())) {
          final var originalStatus = PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
          BeanUtils.copyProperties(entity, penRequestBatchStudent, "penRequestBatchStudentValidationIssueEntities", "penRequestBatchEntity", "penRequestBatchStudentID", "penRequestBatchStudentPossibleMatchEntities");
          final var savedPrbStudent = this.repository.save(penRequestBatchStudent);
          // prb student is updated while unarchived
          boolean isUnArchivedChanged = false;
          if (StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())) {
            penRequestBatch.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode());
            isUnArchivedChanged = true;
          }
          //adjust the summary Count values in the PEN Request Batch
          final var currentStatus = PenRequestBatchStudentStatusCodes.valueOfCode(entity.getPenRequestBatchStudentStatusCode());
          log.debug("The Original status :: {} and Current status :: {} of Student :: {}", originalStatus, currentStatus, savedPrbStudent);
          if (!this.useSameSummaryCounter(originalStatus, currentStatus)) {
            this.logCounts(penRequestBatch, "Current");
            this.changeSummaryCount(penRequestBatch, originalStatus, true);
            this.changeSummaryCount(penRequestBatch, currentStatus, false);
            penRequestBatch.setUpdateUser(penRequestBatchStudent.getUpdateUser());
            penRequestBatch.setUpdateDate(penRequestBatchStudent.getUpdateDate());
            this.saveBatch(penRequestBatch, isUnArchivedChanged);
            this.logCounts(penRequestBatch, "Updated");
          } else if (isUnArchivedChanged) {
            this.saveBatch(penRequestBatch, true);
          }
          return savedPrbStudent;
        } else {
          throw new InvalidParameterException(penRequestBatchID.toString(), penRequestBatchStudentID.toString()); // this student does not belong to the specific batch ID.
        }
      } else {
        throw new EntityNotFoundException(PenRequestBatchStudentEntity.class, "penRequestBatchStudentID", penRequestBatchStudentID.toString());
      }

    } else {
      throw new EntityNotFoundException(PenRequestBatchEntity.class, "penRequestBatchID", penRequestBatchID.toString());
    }
  }

  private void saveBatch(final PenRequestBatchEntity penRequestBatch, final boolean isUnArchivedChanged) {
    if (!isUnArchivedChanged) {
      this.penRequestBatchRepository.save(penRequestBatch);
    } else {
      this.penRequestBatchService.savePenRequestBatch(penRequestBatch);
    }
  }

  private void logCounts(final PenRequestBatchEntity penRequestBatch, final String initialMessage) {
    log.debug(initialMessage.concat(" counts are  Fixable :: {}, Error :: {}, Repeat :: {}, Matched :: {}, New Pen :: {}, Duplicate :: {}"),
      penRequestBatch.getFixableCount(),
      penRequestBatch.getErrorCount(),
      penRequestBatch.getRepeatCount(),
      penRequestBatch.getMatchedCount(),
      penRequestBatch.getNewPenCount(),
      penRequestBatch.getDuplicateCount());
  }

  private boolean useSameSummaryCounter(final PenRequestBatchStudentStatusCodes originalStatus, final PenRequestBatchStudentStatusCodes currentStatus) {
    return originalStatus.equals(currentStatus) ||
      ((originalStatus.equals(ERROR) || originalStatus.equals(INFOREQ)) && (currentStatus.equals(ERROR) || currentStatus.equals(INFOREQ))) ||
      ((originalStatus.equals(SYS_MATCHED) || originalStatus.equals(USR_MATCHED)) && (currentStatus.equals(SYS_MATCHED) || currentStatus.equals(USR_MATCHED))) ||
      ((originalStatus.equals(SYS_NEW_PEN) || originalStatus.equals(USR_NEW_PEN)) && (currentStatus.equals(SYS_NEW_PEN) || currentStatus.equals(USR_NEW_PEN)));

  }

  private void changeSummaryCount(final PenRequestBatchEntity penRequestBatch, final PenRequestBatchStudentStatusCodes status, final boolean changeFrom) {
    final int count = changeFrom ? -1 : 1;
    switch (status) {
      case FIXABLE:
        penRequestBatch.setFixableCount((penRequestBatch.getFixableCount() != null ? penRequestBatch.getFixableCount() : 0) + count);
        break;
      case SYS_MATCHED:
      case USR_MATCHED:
        penRequestBatch.setMatchedCount((penRequestBatch.getMatchedCount() != null ? penRequestBatch.getMatchedCount() : 0) + count);
        break;
      case INFOREQ:
      case ERROR:
        penRequestBatch.setErrorCount((penRequestBatch.getErrorCount() != null ? penRequestBatch.getErrorCount() : 0) + count);
        break;
      case REPEAT:
        penRequestBatch.setRepeatCount((penRequestBatch.getRepeatCount() != null ? penRequestBatch.getRepeatCount() : 0) + count);
        break;
      case SYS_NEW_PEN:
      case USR_NEW_PEN:
        penRequestBatch.setNewPenCount((penRequestBatch.getNewPenCount() != null ? penRequestBatch.getNewPenCount() : 0) + count);
        break;
      case DUPLICATE:
        penRequestBatch.setDuplicateCount((penRequestBatch.getDuplicateCount() != null ? penRequestBatch.getDuplicateCount() : 0) + count);
        break;
      default:
    }

  }

  /**
   * Gets student by id.
   *
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the student by id
   */
  public PenRequestBatchStudentEntity getStudentById(final UUID penRequestBatchID, final UUID penRequestBatchStudentID) {
    final var penRequestBatchStudentOptional = this.getRepository().findById(penRequestBatchStudentID);
    if (penRequestBatchStudentOptional.isPresent()) {
      final var penRequestBatchStudent = penRequestBatchStudentOptional.get();
      if (penRequestBatchStudent.getPenRequestBatchEntity().getPenRequestBatchID().equals(penRequestBatchID)) {
        return penRequestBatchStudent;
      } else {
        throw new InvalidParameterException(penRequestBatchID.toString(), penRequestBatchStudentID.toString()); // this student does not belong to the specific batch ID.
      }
    } else {
      throw new EntityNotFoundException(PenRequestBatchStudentEntity.class, penRequestBatchStudentID.toString());
    }
  }


  /**
   * Find all students requests that are repeats of the current student request
   *
   * @param penRequestBatchStudent the student request
   * @return the list
   */
  public List<PenRequestBatchStudentEntity> findAllRepeatsGivenBatchStudent(final PenRequestBatchStudentEntity penRequestBatchStudent) {
    final int repeatTimeWindow;
    if (penRequestBatchStudent.getPenRequestBatchEntity().getSchoolGroupCode().equals(SchoolGroupCodes.PSI.getCode())) {
      repeatTimeWindow = this.getApplicationProperties().getRepeatTimeWindowPSI();
    } else {
      repeatTimeWindow = this.getApplicationProperties().getRepeatTimeWindowK12();
    }
    final LocalDateTime startDate = LocalDateTime.now().minusDays(repeatTimeWindow);
    return this.repository.findAllRepeatsGivenBatchStudent(penRequestBatchStudent.getPenRequestBatchEntity().getMincode(), PenRequestBatchStatusCodes.ARCHIVED.getCode(), startDate, penRequestBatchStudent.getLocalID(), Arrays.asList(PenRequestBatchStudentStatusCodes.FIXABLE.getCode(), ERROR.getCode(), PenRequestBatchStudentStatusCodes.LOADED.getCode()), penRequestBatchStudent.getSubmittedPen(), penRequestBatchStudent.getLegalFirstName(), penRequestBatchStudent.getLegalMiddleNames(), penRequestBatchStudent.getLegalLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames(), penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getDob(), penRequestBatchStudent.getGenderCode(), penRequestBatchStudent.getGradeCode(), penRequestBatchStudent.getPostalCode());
  }

  /**
   * Find all completable future.
   *
   * @param studentEntitySpecification the student entity specification
   * @param pageNumber                 the page number
   * @param pageSize                   the page size
   * @param sorts                      the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<PenRequestBatchStudentEntity>> findAll(final Specification<PenRequestBatchStudentEntity> studentEntitySpecification, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.getRepository().findAll(studentEntitySpecification, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }


  /**
   * Gets all student status codes.
   *
   * @return the all student status codes
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchStudentStatusCodeEntity> getAllStudentStatusCodes() {
    return this.getStudentStatusCodeRepository().findAll();
  }

  /**
   * Find by id optional.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the optional
   */
  public Optional<PenRequestBatchStudentEntity> findByID(final UUID penRequestBatchStudentID) {
    return this.getRepository().findById(penRequestBatchStudentID);
  }

  public Map<String, List<PenRequestBatchStudentEntity>> populateRepeatCheckMap(@NonNull final PenRequestBatchEntity penRequestBatchEntity) {
    final Map<String, List<PenRequestBatchStudentEntity>> repeatedEntityMap = new HashMap<>();
    final int repeatTimeWindow;
    if (penRequestBatchEntity.getSchoolGroupCode().equals(SchoolGroupCodes.PSI.getCode())) {
      repeatTimeWindow = this.getApplicationProperties().getRepeatTimeWindowPSI();
    } else {
      repeatTimeWindow = this.getApplicationProperties().getRepeatTimeWindowK12();
    }
    final LocalDateTime startDate = LocalDateTime.now().minusDays(repeatTimeWindow);
    val result = this.repository.findAllPenRequestBatchStudentsForGivenCriteria(penRequestBatchEntity.getMincode(),
      PenRequestBatchStatusCodes.ARCHIVED.getCode(), startDate,
      Arrays.asList(FIXABLE.getCode(), ERROR.getCode(), LOADED.getCode(), INFOREQ.getCode()));
    if (result.isEmpty()) {
      return Collections.emptyMap();
    }
    for (val prbStudentEntity : result) {
      final String key = this.constructKeyGivenBatchStudent(prbStudentEntity);
      if (repeatedEntityMap.containsKey(key)) {
        val entries = repeatedEntityMap.get(key);
        entries.add(prbStudentEntity);
        repeatedEntityMap.put(key, entries);
      } else {
        val entries = new ArrayList<PenRequestBatchStudentEntity>();
        entries.add(prbStudentEntity);
        repeatedEntityMap.put(key, entries);
      }
    }
    return repeatedEntityMap;
  }

  public String constructKeyGivenBatchStudent(@NonNull final PenRequestBatchStudentEntity prbsEntity) {
    return prbsEntity.getLocalID() + prbsEntity.getSubmittedPen() + prbsEntity.getLegalFirstName() + prbsEntity.getLegalMiddleNames() + prbsEntity.getLegalLastName() + prbsEntity.getUsualFirstName() + prbsEntity.getUsualMiddleNames() + prbsEntity.getUsualLastName() + prbsEntity.getDob() + prbsEntity.getGenderCode() + prbsEntity.getGradeCode() + prbsEntity.getPostalCode();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean isPenAlreadyAssigned(final PenRequestBatchEntity penRequestBatch, final String assignedPen) {
    boolean penAlreadyAssigned = false;
    val redisKey = "multiple-assigned-pen-check::".concat(penRequestBatch.getPenRequestBatchID().toString()).concat("::").concat(assignedPen);
    log.debug("checking for multiples in batch:: {}", penRequestBatch.getPenRequestBatchID());
    final RPermitExpirableSemaphore semaphore = this.getRedissonClient().getPermitExpirableSemaphore("checkForMultiple::" + penRequestBatch.getPenRequestBatchID());
    semaphore.trySetPermits(1);
    semaphore.expire(120, TimeUnit.SECONDS);
    try {
      final String id = semaphore.tryAcquire(120, 40, TimeUnit.SECONDS);
      final String assignedPEN = this.getStringRedisTemplate().opsForValue().get(redisKey);
      if (StringUtils.isNotBlank(assignedPEN)) {
        penAlreadyAssigned = true;
      } else {
        this.getStringRedisTemplate().opsForValue().set(redisKey, "true", Duration.ofDays(1));
      }
      semaphore.tryRelease(id);
    } catch (final Exception e) {
      log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      throw new PenRegAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
    }
    return penAlreadyAssigned;
  }

  /**
   * Find all of the same PEN numbers issued to more than one student within a list of pen request batch ids.
   *
   * @param penRequestBatchID the pen request batch id
   * @return a list of pen batch request student ids that have the same assigned pen number.
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchStudentEntity> getAllSamePensWithinPenRequestBatchByID(final List<UUID> penRequestBatchID) {
    return this.getRepository().findSameAssignedPensByPenRequestBatchID(penRequestBatchID);
  }

}
