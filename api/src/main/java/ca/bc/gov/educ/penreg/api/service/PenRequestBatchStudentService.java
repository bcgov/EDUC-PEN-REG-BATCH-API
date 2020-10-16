package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentPossibleMatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentStatusCodeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
   * Save all students iterable.
   *
   * @param penRequestBatchStudentEntities the pen request batch student entities
   * @return the iterable
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Iterable<PenRequestBatchStudentEntity> saveAllStudents(final List<PenRequestBatchStudentEntity> penRequestBatchStudentEntities) {
    return getRepository().saveAll(penRequestBatchStudentEntities);
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
   *
   * @param entity                   the entity
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student entity
   */
  public PenRequestBatchStudentEntity updateStudent(PenRequestBatchStudentEntity entity, UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    var penRequestBatchOptional = getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      var penRequestBatch = penRequestBatchOptional.get();
      var penRequestBatchStudentOptional = getRepository().findById(penRequestBatchStudentID);
      if (penRequestBatchStudentOptional.isPresent()) {
        var penRequestBatchStudent = penRequestBatchStudentOptional.get();
        if (penRequestBatchStudent.getPenRequestBatchEntity().getPenRequestBatchID().equals(penRequestBatch.getPenRequestBatchID())) {
          BeanUtils.copyProperties(entity, penRequestBatchStudent, "penRequestBatchStudentValidationIssueEntities", "penRequestBatchEntity", "penRequestBatchStudentID", "penRequestBatchStudentPossibleMatchEntities");
          return repository.save(penRequestBatchStudent);
        } else {
          throw new InvalidParameterException(penRequestBatchID.toString(), penRequestBatchStudentID.toString()); // this student does not belong to the specific batch ID.
        }
      } else {
        throw new EntityNotFoundException(PenRequestBatchStudentEntity.class, penRequestBatchStudentID.toString());
      }

    } else {
      throw new EntityNotFoundException(PenRequestBatchEntity.class, penRequestBatchID.toString());
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
   * Find all student by pen request batch id list.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the list
   */
  public List<PenRequestBatchStudentEntity> findAllStudentByPenRequestBatchID(UUID penRequestBatchID) {
    var penRequestBatchOptional = getPenRequestBatchRepository().findById(penRequestBatchID);
    if (penRequestBatchOptional.isPresent()) {
      return getRepository().findAllByPenRequestBatchEntity(penRequestBatchOptional.get());
    } else {
      throw new EntityNotFoundException(PenRequestBatchStudentEntity.class, penRequestBatchID.toString());
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
    if(penRequestBatchStudent.getPenRequestBatchEntity().getSchoolGroupCode().equals(SchoolGroupCodes.PSI.getCode())){
      repeatTimeWindow = getApplicationProperties().getRepeatTimeWindowPSI();
    }else {
      repeatTimeWindow = getApplicationProperties().getRepeatTimeWindowK12();
    }
    LocalDateTime startDate = LocalDateTime.now().minusDays(repeatTimeWindow);
    return repository.findAllRepeatsGivenBatchStudent(penRequestBatchStudent.getPenRequestBatchEntity().getMinCode(), PenRequestBatchStatusCodes.ARCHIVED.getCode(), startDate, penRequestBatchStudent.getLocalID(), Arrays.asList(PenRequestBatchStudentStatusCodes.FIXABLE.getCode(), PenRequestBatchStudentStatusCodes.ERROR.getCode(), PenRequestBatchStudentStatusCodes.LOADED.getCode()), penRequestBatchStudent.getSubmittedPen(), penRequestBatchStudent.getLegalFirstName(), penRequestBatchStudent.getLegalMiddleNames(), penRequestBatchStudent.getLegalLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames(), penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getDob(), penRequestBatchStudent.getGenderCode(), penRequestBatchStudent.getGradeCode(), penRequestBatchStudent.getPostalCode());
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
    Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
    try {
      var result = getRepository().findAll(studentEntitySpecification, paging);
      return CompletableFuture.completedFuture(result);
    } catch (final Exception ex) {
      throw new CompletionException(ex);
    }
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

  /**
   * Gets all possible matches.
   *
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the all possible matches
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public Set<PenRequestBatchStudentPossibleMatchEntity> getAllPossibleMatches(UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    var penRequestBatchStudentOptional = getRepository().findById(penRequestBatchStudentID);
    if (penRequestBatchStudentOptional.isPresent()) {
      var student = penRequestBatchStudentOptional.get();
      if (!student.getPenRequestBatchEntity().getPenRequestBatchID().equals(penRequestBatchID)) {
        throw new EntityNotFoundException(PenRequestBatchStudentEntity.class);
      }
      return student.getPenRequestBatchStudentPossibleMatchEntities();
    } else {
      throw new EntityNotFoundException(PenRequestBatchStudentEntity.class);
    }
  }
}
