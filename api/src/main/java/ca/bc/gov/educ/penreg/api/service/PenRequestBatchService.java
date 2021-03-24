package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.model.v1.*;
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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
   * the pen coordinator service
   */
  @Getter(PRIVATE)
  private final PenCoordinatorService penCoordinatorService;

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
  public PenRequestBatchService(final PenRequestBatchRepository repository, final PenWebBlobRepository penWebBlobRepository, PenRequestBatchStudentRepository penRequestBatchStudentRepository, final PenCoordinatorService penCoordinatorService, final RestUtils restUtils) {
    this.repository = repository;
    this.penWebBlobRepository = penWebBlobRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.penCoordinatorService = penCoordinatorService;
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
      checkAndPopulateStatusCode(penRequestBatchEntity, penRequestBatchEntityDB);
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
  @Transactional(propagation = Propagation.MANDATORY)
  public PENWebBlobEntity createIDSFile(final PenRequestBatchEntity penRequestBatchEntity) {
    final var penRequestBatchStudentEntities = this.getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsInAndLocalIDNotNull(penRequestBatchEntity, Arrays.asList(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.USR_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode(), PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode()));

    if (penRequestBatchStudentEntities.isEmpty()) {
      return null;
    }

    final StringBuilder idsFile = new StringBuilder();

    for (final PenRequestBatchStudentEntity entity : penRequestBatchStudentEntities) {
      final var student = this.getRestUtils().getStudentByPEN(entity.getAssignedPEN());
      if (student.isPresent()) {
//        Uncomment and update this logic once trueNumber is added to student table
//        if(student.get().getTrueNumber()) {
//          student = getRestUtils().getStudentByStudentID(student.get().getTrueNumber());
//        }
//        if(student.isPresent()) {
        idsFile.append("E03").append(student.get().getMincode()).append(String.format("%-12s", student.get().getLocalID()).replace(' ', '0')).append(student.get().getPen()).append(" ").append(student.get().getLegalLastName()).append("\n");
//        }
      }
    }
    final byte[] bFile = idsFile.toString().getBytes();

    return this.getPenWebBlobRepository().save(PENWebBlobEntity.builder().mincode(penRequestBatchEntity.getMincode()).sourceApplication("PENWEB").fileName(penRequestBatchEntity.getMincode() + ".IDS").fileType("IDS").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(penRequestBatchEntity.getSubmissionNumber()).build());
  }

  /**
   * Create an TXT file for a pen request batch entity
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen web blob entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PENWebBlobEntity createTxtFile(final PenRequestBatchEntity penRequestBatchEntity) {
    final var penRequestBatchStudentEntities = this.getPenRequestBatchStudentRepository().findAllByPenRequestBatchEntityAndPenRequestBatchStudentStatusCodeIsInAndLocalIDNotNull(penRequestBatchEntity, Arrays.asList(PenRequestBatchStudentStatusCodes.ERROR.getCode(), PenRequestBatchStudentStatusCodes.INFOREQ.getCode()));

    if (penRequestBatchStudentEntities.isEmpty()) {
      return null;
    }

    // retrieve the original prb file from school
    List<PENWebBlobEntity> penWebBlobs = findPenWebBlobBySubmissionNumberAndFileType(penRequestBatchEntity.getSubmissionNumber(), "PEN");
    PENWebBlobEntity penWebBlob = penWebBlobs.isEmpty()? null : penWebBlobs.get(0);

    final StringBuilder txtFile = new StringBuilder();
    // FFI header
    txtFile.append(createHeader(penRequestBatchEntity, penWebBlob));

    // SRM details records
    for (final PenRequestBatchStudentEntity entity : penRequestBatchStudentEntities) {
      if (entity.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode())) {
        txtFile.append(createBody(entity, penWebBlob));
      }
    }

    // BTR footer
    txtFile.append(createFooter(penRequestBatchEntity));

    final byte[] bFile = txtFile.toString().getBytes();
    return this.getPenWebBlobRepository().save(PENWebBlobEntity.builder().mincode(penRequestBatchEntity.getMincode()).sourceApplication("PENWEB").fileName(penRequestBatchEntity.getMincode() + ".TXT").fileType("TXT").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(penRequestBatchEntity.getSubmissionNumber()).build());
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
  private String createHeader(final PenRequestBatchEntity penRequestBatchEntity, PENWebBlobEntity penWebBlob) {
    final StringBuilder header = new StringBuilder();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    // retrieved from PEN_COORDINATOR table
    Optional<PenCoordinator> penCoordinator = penCoordinatorService.getPenCoordinatorByMinCode(penRequestBatchEntity.getMincode());
    // retrieved from the original prb file
    String applicationCode = penWebBlob != null? getApplicationCodeFromRawHeader(penWebBlob.getFileContents()) : "";

    header.append("FFI")
            .append(String.format("%-8.8s", print(penRequestBatchEntity.getMincode())))
            .append(String.format("%-40.40s", print(penRequestBatchEntity.getSchoolName())))
            .append(String.format("%-8.8s", dateFormat.format(new Date())))
            .append(String.format("%-100.100s", print(penCoordinator.isPresent()? penCoordinator.get().getPenCoordinatorEmail() : "")))
            .append(String.format("%-10.10s", print(penCoordinator.isPresent()? penCoordinator.get().getPenCoordinatorFax().replaceAll("[^0-9]+","") : "")))
            .append(String.format("%-40.40s", print(penCoordinator.isPresent()? penCoordinator.get().getPenCoordinatorName() : "")))
            .append("  ")
            .append(String.format("%-4.4s", print(applicationCode)))
            .append("\n");

    return header.toString();
  }

  private String createFooter(final PenRequestBatchEntity penRequestBatchEntity) {
    final StringBuilder footer = new StringBuilder();

    footer.append("BTR")
            .append(String.format("%04d", print(penRequestBatchEntity.getSourceStudentCount())))
            .append(String.format("%-100.100s", print(penRequestBatchEntity.getSisVendorName())))
            .append(String.format("%-100.100s", print(penRequestBatchEntity.getSisProductName())))
            .append(String.format("%-15.15s", print(penRequestBatchEntity.getSisProductID())))
            .append("\n");

    return footer.toString();
  }

  private String createBody(final PenRequestBatchStudentEntity penRequestBatchStudentEntity, PENWebBlobEntity penWebBlob) {
    final StringBuilder body = new StringBuilder();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    // retrieved from the original prb file
    String applicationKey = penWebBlob != null? getApplicationKeyFromRawStudentRecords(penWebBlob.getFileContents(), penRequestBatchStudentEntity.getLocalID()) : "";

    body.append("SRM")
            .append(StringUtils.leftPad(penRequestBatchStudentEntity.getLocalID(), 12, "0"))
            .append(String.format("%-10.10s", print(penRequestBatchStudentEntity.getSubmittedPen())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalLastName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalFirstName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalMiddleNames())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualLastName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualFirstName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualMiddleNames())))
            .append(String.format("%-8.8s", print(penRequestBatchStudentEntity.getDob())))
            .append(String.format("%1.1s", print(penRequestBatchStudentEntity.getGenderCode())))
            .append(StringUtils.leftPad("", 16, " "))
            .append(String.format("%-2.2s", print(penRequestBatchStudentEntity.getGradeCode())))
            .append(StringUtils.leftPad("", 26, " "))
            .append(String.format("%-7.7s", print(penRequestBatchStudentEntity.getPostalCode())))
            .append(String.format("%-20.20s", print(applicationKey)))
            .append("\n");

    return body.toString();
  }

  private String getApplicationCodeFromRawHeader(byte[] fileContents) {
    try {
      InputStreamReader inStreamReader = new InputStreamReader(new ByteArrayInputStream(fileContents));
      BufferedReader reader = new BufferedReader(inStreamReader);

      String applicationCode = "";
      String line = "";
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("FFI")) {
          applicationCode = line.substring(211,215).trim();
          break;
        }
      }
      return applicationCode;
    } catch (IOException ioe) {
      return "";
    }
  }

  private String getApplicationKeyFromRawStudentRecords(byte[] fileContents, String localID) {
    try {
      InputStreamReader inStreamReader = new InputStreamReader(new ByteArrayInputStream(fileContents));
      BufferedReader reader = new BufferedReader(inStreamReader);

      String applicationKey = "";
      String line = "";
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("SRM")) {
          String currentLocalID = line.substring(3,15);
          if (StringUtils.equals(currentLocalID, StringUtils.leftPad(localID, 12, "0"))) {
            applicationKey = line.substring(235, 255).trim();
            break;
          }
        }
      }
      return applicationKey;
    } catch (IOException ioe) {
      return "";
    }
  }

  private String print(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }

  private Long print(Long value) {
    if (value == null) {
      return Long.valueOf(0L);
    }
    return value;
  }

  private PenRequestBatchStat calculateFixableAndRepeats(final List<PenRequestBatchEntity> results) {
    final PenRequestBatchStat.PenRequestBatchStatBuilder builder = PenRequestBatchStat.builder();
    long fixableCount = 0;
    long repeatCount = 0;
    for (val result : results) {
      fixableCount += result.getFixableCount();
      repeatCount += result.getRepeatCount();
    }
    builder.fixableCount(fixableCount);
    builder.repeatCount(repeatCount);
    builder.pendingCount((long) results.size());
    return builder.build();
  }

  private void checkAndPopulateStatusCode(PenRequestBatchEntity requestPrbEntity, PenRequestBatchEntity currentPrbEntity) {
    if ( StringUtils.equals(requestPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.ARCHIVED.getCode())
        && (StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())
          || StringUtils.equals(currentPrbEntity.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode())) ) {
      requestPrbEntity.setPenRequestBatchStatusCode(REARCHIVED.getCode());
    }
  }
}
