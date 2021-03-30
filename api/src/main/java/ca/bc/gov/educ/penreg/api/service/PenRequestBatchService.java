package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStats;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStat;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
  public PenRequestBatchService(final PenRequestBatchRepository repository, final PenWebBlobRepository penWebBlobRepository, final PenRequestBatchStudentRepository penRequestBatchStudentRepository, final RestUtils restUtils) {
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
  public Optional<PenRequestBatchEntity> findPenRequestBatchBySubmissionNumber(@NonNull final String submissionNumber) {
    return this.getRepository().findBySubmissionNumber(submissionNumber);
  }

  /**
   * Find pen web blobs by submission number and file type.
   *
   * @param submissionNumber the submission number
   * @param fileType the file type
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

  /**
   * Create an IDS file for a pen request batch entity
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen web blob entity
   */
  public PENWebBlobEntity getIDSBlob(final PenRequestBatchEntity penRequestBatchEntity) {
    final var penRequestBatchStudentEntities = this.getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsInAndLocalIDNotNull(penRequestBatchEntity, Arrays.asList(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.USR_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode(), PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode()));

    if (penRequestBatchStudentEntities.isEmpty()) {
      return null;
    }

    final StringBuilder idsFile = new StringBuilder();

    for (final PenRequestBatchStudentEntity entity : penRequestBatchStudentEntities) {
      final var student = this.getRestUtils().getStudentByPEN(entity.getAssignedPEN());
      student.ifPresent(value -> idsFile.append("E03").append(value.getMincode()).append(String.format("%-12s", value.getLocalID()).replace(' ', '0')).append(value.getPen()).append(" ").append(value.getLegalLastName()).append("\n"));
    }
    final byte[] bFile = idsFile.toString().getBytes();

    return PENWebBlobEntity.builder()
            .mincode(penRequestBatchEntity.getMincode())
            .sourceApplication("PENWEB")
            .fileName(penRequestBatchEntity.getMincode() + ".IDS")
            .fileType("IDS")
            .fileContents(bFile)
            .insertDateTime(LocalDateTime.now())
            .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
            .build();
  }
  public PENWebBlobEntity getPDFBlob(String pdfReport, PenRequestBatchEntity penRequestBatchEntity) {
    return PENWebBlobEntity.builder()
            .mincode(penRequestBatchEntity.getMincode())
            .sourceApplication("PENWEB")
            .fileName(penRequestBatchEntity.getMincode() + ".PDF")
            .fileType("PDF")
            .fileContents(pdfReport.getBytes())
            .insertDateTime(LocalDateTime.now())
            .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
            .build();
  }
  @Transactional(propagation = Propagation.MANDATORY)
  public List<PENWebBlobEntity> saveReports(final String pdfReport, PenRequestBatchEntity penRequestBatchEntity) {
    return this.getPenWebBlobRepository().saveAll(
            Arrays.asList(
                    this.getPDFBlob(pdfReport, penRequestBatchEntity),
                    this.getIDSBlob(penRequestBatchEntity))
    );
  }

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

  private void checkAndPopulateStatusCode(final PenRequestBatchEntity requestPrbEntity, final PenRequestBatchEntity currentPrbEntity) {
    if (StringUtils.equals(requestPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.ARCHIVED.getCode())
        && (StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())
        || StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode()))) {
      requestPrbEntity.setPenRequestBatchStatusCode(REARCHIVED.getCode());
    }
  }
}
