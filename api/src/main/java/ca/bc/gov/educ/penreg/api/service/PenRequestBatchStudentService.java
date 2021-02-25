package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentStatusCodeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
   * The Student status code repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository;

  /**
   * The Application properties.
   */
  @Getter
  private final ApplicationProperties applicationProperties;

  /**
   * Instantiates a new Pen request batch student service.
   *
   * @param repository                  the repository
   * @param penRequestBatchRepository   the pen request batch repository
   * @param studentStatusCodeRepository the student status code repository
   * @param applicationProperties       the application properties
   */
  @Autowired
  public PenRequestBatchStudentService(PenRequestBatchStudentRepository repository, PenRequestBatchRepository penRequestBatchRepository, PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository, ApplicationProperties applicationProperties) {
    this.repository = repository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.studentStatusCodeRepository = studentStatusCodeRepository;
    this.applicationProperties = applicationProperties;
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
    var penRequestBatchOptional = getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      entity.setPenRequestBatchEntity(penRequestBatchOptional.get());
      return repository.save(entity);
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
    return repository.save(entity);
  }

  /**
   * BE CAREFUL while changing the transaction propagation.
   *
   * @param entity the entity
   */
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveAttachedEntityWithChildEntities(final PenRequestBatchStudentEntity entity) {
    repository.save(entity);
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
  public PenRequestBatchStudentEntity updateStudent(PenRequestBatchStudentEntity entity, UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    var penRequestBatchOptional = getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      var penRequestBatch = penRequestBatchOptional.get();
      var penRequestBatchStudentOptional = getRepository().findById(penRequestBatchStudentID);
      if (penRequestBatchStudentOptional.isPresent()) {
        var penRequestBatchStudent = penRequestBatchStudentOptional.get();
        if (penRequestBatchStudent.getPenRequestBatchEntity().getPenRequestBatchID().equals(penRequestBatch.getPenRequestBatchID())) {
          var originalStatus = PenRequestBatchStudentStatusCodes.valueOfCode(penRequestBatchStudent.getPenRequestBatchStudentStatusCode());
          BeanUtils.copyProperties(entity, penRequestBatchStudent, "penRequestBatchStudentValidationIssueEntities", "penRequestBatchEntity", "penRequestBatchStudentID", "penRequestBatchStudentPossibleMatchEntities");
          var savedPrbStudent = repository.save(penRequestBatchStudent);
          // prb student is updated while unarchived
          boolean isUnArchivedChanged = false;
          if (StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())) {
            penRequestBatch.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode());
            isUnArchivedChanged = true;
          }
          //adjust the summary Count values in the PEN Request Batch
          var currentStatus = PenRequestBatchStudentStatusCodes.valueOfCode(entity.getPenRequestBatchStudentStatusCode());
          log.debug("The Original status :: {} and Current status :: {} of Student :: {}", originalStatus, currentStatus, savedPrbStudent);
          if (!useSameSummaryCounter(originalStatus, currentStatus)) {
            logCounts(penRequestBatch, "Current");
            changeSummaryCount(penRequestBatch, originalStatus, true);
            changeSummaryCount(penRequestBatch, currentStatus, false);
            penRequestBatch.setUpdateUser(penRequestBatchStudent.getUpdateUser());
            penRequestBatch.setUpdateDate(penRequestBatchStudent.getUpdateDate());
            penRequestBatchRepository.save(penRequestBatch);
            logCounts(penRequestBatch, "Updated");
          } else if (isUnArchivedChanged) {
            penRequestBatchRepository.save(penRequestBatch);
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

  private void logCounts(PenRequestBatchEntity penRequestBatch, String initialMessage) {
    log.debug(initialMessage.concat(" counts are  Fixable :: {}, Error :: {}, Repeat :: {}, Matched :: {}, New Pen :: {}"),
        penRequestBatch.getFixableCount(),
        penRequestBatch.getErrorCount(),
        penRequestBatch.getRepeatCount(),
        penRequestBatch.getMatchedCount(),
        penRequestBatch.getNewPenCount());
  }

  private boolean useSameSummaryCounter(PenRequestBatchStudentStatusCodes originalStatus, PenRequestBatchStudentStatusCodes currentStatus) {
    return originalStatus.equals(currentStatus) ||
        ((originalStatus.equals(ERROR) || originalStatus.equals(INFOREQ)) && (currentStatus.equals(ERROR) || currentStatus.equals(INFOREQ))) ||
        ((originalStatus.equals(SYS_MATCHED) || originalStatus.equals(USR_MATCHED)) && (currentStatus.equals(SYS_MATCHED) || currentStatus.equals(USR_MATCHED))) ||
        ((originalStatus.equals(SYS_NEW_PEN) || originalStatus.equals(USR_NEW_PEN)) && (currentStatus.equals(SYS_NEW_PEN) || currentStatus.equals(USR_NEW_PEN)));

  }

  private void changeSummaryCount(PenRequestBatchEntity penRequestBatch, PenRequestBatchStudentStatusCodes status, boolean changeFrom) {
    int count = changeFrom ? -1 : 1;
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
  public PenRequestBatchStudentEntity getStudentById(UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    var penRequestBatchStudentOptional = getRepository().findById(penRequestBatchStudentID);
    if (penRequestBatchStudentOptional.isPresent()) {
      var penRequestBatchStudent = penRequestBatchStudentOptional.get();
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
  public List<PenRequestBatchStudentEntity> findAllRepeatsGivenBatchStudent(PenRequestBatchStudentEntity penRequestBatchStudent) {
    int repeatTimeWindow;
    if (penRequestBatchStudent.getPenRequestBatchEntity().getSchoolGroupCode().equals(SchoolGroupCodes.PSI.getCode())) {
      repeatTimeWindow = getApplicationProperties().getRepeatTimeWindowPSI();
    } else {
      repeatTimeWindow = getApplicationProperties().getRepeatTimeWindowK12();
    }
    LocalDateTime startDate = LocalDateTime.now().minusDays(repeatTimeWindow);
    return repository.findAllRepeatsGivenBatchStudent(penRequestBatchStudent.getPenRequestBatchEntity().getMincode(), PenRequestBatchStatusCodes.ARCHIVED.getCode(), startDate, penRequestBatchStudent.getLocalID(), Arrays.asList(PenRequestBatchStudentStatusCodes.FIXABLE.getCode(), ERROR.getCode(), PenRequestBatchStudentStatusCodes.LOADED.getCode()), penRequestBatchStudent.getSubmittedPen(), penRequestBatchStudent.getLegalFirstName(), penRequestBatchStudent.getLegalMiddleNames(), penRequestBatchStudent.getLegalLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames(), penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getDob(), penRequestBatchStudent.getGenderCode(), penRequestBatchStudent.getGradeCode(), penRequestBatchStudent.getPostalCode());
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
  public CompletableFuture<Page<PenRequestBatchStudentEntity>> findAll(Specification<PenRequestBatchStudentEntity> studentEntitySpecification, Integer pageNumber, Integer pageSize, List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return getRepository().findAll(studentEntitySpecification, paging);
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
    return getStudentStatusCodeRepository().findAll();
  }

  /**
   * Find by id optional.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the optional
   */
  public Optional<PenRequestBatchStudentEntity> findByID(UUID penRequestBatchStudentID) {
    return getRepository().findById(penRequestBatchStudentID);
  }

}
