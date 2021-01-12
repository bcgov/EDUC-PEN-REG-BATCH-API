package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
   * The Rest utils.
   */
  @Getter(PRIVATE)
  private final RestUtils restUtils;

  /**
   * Instantiates a new Pen reg batch service.
   *
   * @param repository           the repository
   * @param penWebBlobRepository the pen web blob repository
   */
  @Autowired
  public PenRequestBatchService(PenRequestBatchRepository repository, PenWebBlobRepository penWebBlobRepository, PenRequestBatchStudentRepository penRequestBatchStudentRepository, RestUtils restUtils) {
    this.repository = repository;
    this.penWebBlobRepository = penWebBlobRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.restUtils = restUtils;
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
  public CompletableFuture<Page<PenRequestBatchEntity>> findAll(Specification<PenRequestBatchEntity> penRegBatchSpecs, Integer pageNumber, Integer pageSize, List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return getRepository().findAll(penRegBatchSpecs, paging);
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
    return getRepository().findById(penRequestBatchID);
  }

  /**
   * Create pen request batch pen request batch entity.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchEntity createPenRequestBatch(final PenRequestBatchEntity penRequestBatchEntity) {
    return getRepository().save(penRequestBatchEntity);
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
    var penRequestBatchEntityOptional = getRepository().findById(penRequestBatchID);
    return penRequestBatchEntityOptional.map(penRequestBatchEntityDB -> {
      BeanUtils.copyProperties(penRequestBatchEntity, penRequestBatchEntityDB,
        "penRequestBatchStudentEntities", "penRequestBatchHistoryEntities", "createUser", "createDate");
      penRequestBatchEntityDB.setPenRequestBatchID(penRequestBatchID);
      return getRepository().save(penRequestBatchEntityDB);
    }).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Save pen request batch pen request batch entity.
   *
   * @param entity the entity
   * @return pen request batch entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PenRequestBatchEntity saveAttachedEntity(final PenRequestBatchEntity entity) {
    return getRepository().save(entity);
  }

  /**
   * Find pen request batch by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public Optional<PenRequestBatchEntity> findPenRequestBatchBySubmissionNumber(@NonNull final String submissionNumber) {
    return getRepository().findBySubmissionNumber(submissionNumber);
  }

  /**
   * Find pen web blob by submission number optional.
   *
   * @param submissionNumber the submission number
   * @return the optional
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public Optional<PENWebBlobEntity> findPenWebBlobBySubmissionNumber(@NonNull final String submissionNumber) {
    return getPenWebBlobRepository().findBySubmissionNumber(submissionNumber);
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
    var penWebBlobEntity = getPenWebBlobRepository().findById(penwebBlobID).map(dbEntity -> {
      dbEntity.setExtractDateTime(entity.getExtractDateTime());
      return dbEntity;
    }).orElseThrow(EntityNotFoundException::new);
    return getPenWebBlobRepository().save(penWebBlobEntity);
  }

  /**
   * Delete pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void deletePenRequestBatch(final UUID penRequestBatchID) {
    var penRequestBatchEntity = getRepository().findById(penRequestBatchID).orElseThrow(EntityNotFoundException::new);
    getRepository().delete(penRequestBatchEntity);
  }

  /**
   * Find by id optional.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the optional
   */
  public Optional<PenRequestBatchEntity> findById(UUID penRequestBatchID) {
    return repository.findById(penRequestBatchID);
  }

  /**
   * Create an IDS file for a pen request batch entity
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen web blob entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PENWebBlobEntity createIDSFile(final PenRequestBatchEntity penRequestBatchEntity) {
    var penRequestBatchStudentEntities = getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsInAndLocalIDNotNull(penRequestBatchEntity, Arrays.asList(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.USR_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode(), PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode()));

    if(penRequestBatchStudentEntities.isEmpty()) {
      return null;
    }

    StringBuilder idsFile = new StringBuilder();

    for(PenRequestBatchStudentEntity entity: penRequestBatchStudentEntities) {
      var student = getRestUtils().getStudentByPEN(entity.getAssignedPEN());
      if(student.isPresent()) {
//        Uncomment and update this logic once trueNumber is added to student table
//        if(student.get().getTrueNumber()) {
//          student = getRestUtils().getStudentByStudentID(student.get().getTrueNumber());
//        }
//        if(student.isPresent()) {
          idsFile.append("E03").append(student.get().getMincode()).append(String.format("%-12s", student.get().getLocalID()).replace(' ', '0')).append(student.get().getPen()).append(" ").append(student.get().getLegalLastName()).append("\n");
//        }
      }
    }
    byte [] bFile = idsFile.toString().getBytes();

    return getPenWebBlobRepository().save(PENWebBlobEntity.builder().mincode(penRequestBatchEntity.getMincode()).sourceApplication("PENWEB").fileName(penRequestBatchEntity.getMincode() + ".IDS").fileType("IDS").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(penRequestBatchEntity.getSubmissionNumber()).build());
  }
}
