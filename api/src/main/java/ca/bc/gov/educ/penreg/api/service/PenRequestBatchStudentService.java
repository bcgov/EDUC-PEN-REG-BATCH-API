package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentStatusCodeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
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

  @Getter(PRIVATE)
  private final PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository;

  /**
   * Instantiates a new Pen request batch student service.
   *
   * @param repository                the repository
   * @param penRequestBatchRepository the pen request batch repository
   */
  @Autowired
  public PenRequestBatchStudentService(PenRequestBatchStudentRepository repository, PenRequestBatchRepository penRequestBatchRepository, PenRequestBatchStudentStatusCodeRepository studentStatusCodeRepository) {
    this.repository = repository;
    this.penRequestBatchRepository = penRequestBatchRepository;
    this.studentStatusCodeRepository = studentStatusCodeRepository;
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
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchStudentEntity saveAttachedEntity(final PenRequestBatchStudentEntity entity) {
    return repository.save(entity);
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
          BeanUtils.copyProperties(entity, penRequestBatchStudent);
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


  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  @Cacheable("penRequestBatchStudentStatusCodes")
  public List<PenRequestBatchStudentStatusCodeEntity> getAllStudentStatusCodes() {
    return getStudentStatusCodeRepository().findAll();
  }
}
