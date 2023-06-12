package ca.bc.gov.educ.penreg.api.controller.v1;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.exception.errors.ApiError;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchFilterSpecs;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchStudentFilterSpecs;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
import ca.bc.gov.educ.penreg.api.mappers.PenRequestBatchStudentValidationIssueMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenWebBlobMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.StudentStatusCodeMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.external.PenRequestBatchResultDataMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentValidationIssueService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStats;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmission;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmissionResult;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


/**
 * Student controller
 *
 * @author Om
 */
@RestController
@Slf4j
public class PenRequestBatchAPIController extends PaginatedController implements PenRequestBatchAPIEndpoint {
  /**
   * The constant batchResultMapper.
   */
  private static final PenRequestBatchResultDataMapper batchResultMapper = PenRequestBatchResultDataMapper.mapper;
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  /**
   * The constant studentMapper.
   */
  private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;
  /**
   * The constant penWebBlobMapper.
   */
  private static final PenWebBlobMapper penWebBlobMapper = PenWebBlobMapper.mapper;
  /**
   * The constant studentStatusCodeMapper.
   */
  private static final StudentStatusCodeMapper studentStatusCodeMapper = StudentStatusCodeMapper.mapper;
  /**
   * The Pen reg batch filter specs.
   */
  @Getter(PRIVATE)
  private final PenRegBatchFilterSpecs penRegBatchFilterSpecs;
  /**
   * The Pen reg batch student filter specs.
   */
  @Getter(PRIVATE)
  private final PenRegBatchStudentFilterSpecs penRegBatchStudentFilterSpecs;
  /**
   * The Service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchService service;
  /**
   * The Student service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentService studentService;
  /**
   * The Saga service
   */
  @Getter(PRIVATE)
  private final SagaService sagaService;
  /**
   * The Pen request batch student validation issue service.
   */
  private final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService;

  /**
   * Instantiates a new Pen request batch api controller.
   *
   * @param penRegBatchFilterSpecs                       the pen reg batch filter specs
   * @param penRegBatchStudentFilterSpecs                the pen reg batch student filter specs
   * @param service                                      the service
   * @param studentService                               the student service
   * @param sagaService                                  the saga service
   * @param penRequestBatchStudentValidationIssueService the pen request batch student validation issue service
   */
  @Autowired
  public PenRequestBatchAPIController(final PenRegBatchFilterSpecs penRegBatchFilterSpecs,
                                      final PenRegBatchStudentFilterSpecs penRegBatchStudentFilterSpecs,
                                      final PenRequestBatchService service,
                                      final PenRequestBatchStudentService studentService,
                                      final SagaService sagaService, final PenRequestBatchStudentValidationIssueService penRequestBatchStudentValidationIssueService) {
    this.penRegBatchFilterSpecs = penRegBatchFilterSpecs;
    this.penRegBatchStudentFilterSpecs = penRegBatchStudentFilterSpecs;
    this.service = service;
    this.studentService = studentService;
    this.sagaService = sagaService;
    this.penRequestBatchStudentValidationIssueService = penRequestBatchStudentValidationIssueService;
  }

  /**
   * Read pen request batch pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @Override
  public PenRequestBatch readPenRequestBatch(final UUID penRequestBatchID) {
    return this.getService().getPenRequestBatchEntityByID(penRequestBatchID).map(mapper::toStructure).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Find all of the same PEN numbers issued to more than one student within one or more pen request batches.
   *
   * @param penRequestBatchID the pen request batch ids within a comma separated string of batches
   * @return a list of the same PEN numbers that were issued to more than one student.
   */
  @Override
  public List<String> findAllSamePensWithinPenRequestBatchByID(final String penRequestBatchID) {
    return this.getStudentService().getAllSamePensWithinPenRequestBatchByID(penRequestBatchID);
  }

  /**
   * Create pen request batch pen request batch.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch
   */
  @Override
  public PenRequestBatch createPenRequestBatch(final PenRequestBatch penRequestBatch) {
    final var model = mapper.toModel(penRequestBatch);
    this.populateAuditColumns(model);
    return mapper.toStructure(this.getService().savePenRequestBatch(model));
  }


  /**
   * Update pen request batch pen request batch.
   *
   * @param penRequestBatch   the pen request batch
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @Override
  public ResponseEntity<PenRequestBatch> updatePenRequestBatch(final PenRequestBatch penRequestBatch, final UUID penRequestBatchID) {
    final var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(List.of(penRequestBatchID), this.getStatusesFilter()).isEmpty();
    if (sagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    final var model = mapper.toModel(penRequestBatch);
    this.populateAuditColumns(model);
    return ResponseEntity.ok(mapper.toStructure(this.getService().updatePenRequestBatch(model, penRequestBatchID)));
  }

  /**
   * Find all completable future.
   *
   * @param pageNumber       the page number
   * @param pageSize         the page size
   * @param sortCriteriaJson the sort criteria json
   * @param searchList       the search list
   * @return the completable future
   */
  @Override
  public CompletableFuture<Page<PenRequestBatchSearch>> findAll(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchList) {

    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchEntity> penRegBatchSpecs = null;
    final Associations associationNames;
    try {
      associationNames = this.getSortCriteria(sortCriteriaJson, JsonUtil.mapper, sorts);
      if (StringUtils.isNotBlank(searchList)) {
        final List<Search> searches = JsonUtil.mapper.readValue(searchList, new TypeReference<>() {
        });
        this.getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (final var search : searches) {
          penRegBatchSpecs = this.getSpecifications(penRegBatchSpecs, i, search, associationNames, this.getPenRegBatchFilterSpecs());
          i++;
        }

      }
    } catch (final JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }

    if (associationNames.hasSearchAssociations()) {
      return this.getService().findAllByPenRequestBatchStudent(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(page -> page.map(pair -> {
        final var batch = mapper.toSearchStructure(pair.getFirst());
        batch.setSearchedCount(pair.getSecond());
        return batch;
      }));
    } else {
      return this.getService().findAll(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(page ->
        page.map(mapper::toSearchStructure));
    }
  }

  /**
   * Create pen request batch student pen request batch student.
   *
   * @param penRequestBatchStudent the pen request batch student
   * @param penRequestBatchID      the pen request batch id
   * @return the pen request batch student
   */
  @Override
  public PenRequestBatchStudent createPenRequestBatchStudent(final PenRequestBatchStudent penRequestBatchStudent, final UUID penRequestBatchID) {
    final var model = studentMapper.toModel(penRequestBatchStudent);
    this.populateAuditColumnsForStudent(model);
    return studentMapper.toStructure(this.getStudentService().createStudent(model, penRequestBatchID));
  }


  /**
   * Update pen request batch student pen request batch student.
   *
   * @param penRequestBatchStudent   the pen request batch student
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student
   */
  @Override
  public PenRequestBatchStudent updatePenRequestBatchStudent(final PenRequestBatchStudent penRequestBatchStudent, final UUID penRequestBatchID, final UUID penRequestBatchStudentID) {
    final var model = studentMapper.toModel(penRequestBatchStudent);
    this.populateAuditColumnsForStudent(model);
    return studentMapper.toStructure(this.getStudentService().updateStudent(model, penRequestBatchID, penRequestBatchStudentID));
  }

  /**
   * Gets pen request batch student by id.
   *
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student by id
   */
  @Override
  public PenRequestBatchStudent getPenRequestBatchStudentByID(final UUID penRequestBatchID, final UUID penRequestBatchStudentID) {
    return studentMapper.toStructure(this.getStudentService().getStudentById(penRequestBatchID, penRequestBatchStudentID));
  }

  /**
   * Gets the list of  pen request batch by submission number.
   *
   * @param submissionNumber the submission number
   * @return the pen request batch by submission number
   */
  @Override
  public List<PenRequestBatch> getPenRequestBatchBySubmissionNumber(final String submissionNumber) {
    return this.getService().findPenRequestBatchBySubmissionNumber(submissionNumber).stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  /**
   * Delete pen request batch response entity.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the response entity
   */
  @Override
  public ResponseEntity<Void> deletePenRequestBatch(final UUID penRequestBatchID) {
    this.getService().deletePenRequestBatch(penRequestBatchID);
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets list of pen web blob by submission number and file type.
   *
   * @param submissionNumber the submission number
   * @param fileType         the file type
   * @return the list of pen web blob by submission number and file type
   */
  @Override
  public List<PENWebBlob> getPenWebBlobs(final String submissionNumber, final String fileType) {
    final List<PENWebBlobEntity> blobEntities;
    if (fileType == null) {
      blobEntities = this.getService().findPenWebBlobBySubmissionNumber(submissionNumber);
    } else {
      blobEntities = this.getService().findPenWebBlobBySubmissionNumberAndFileType(submissionNumber, fileType);
    }
    return blobEntities.stream().map(penWebBlobMapper::toStructure).collect(Collectors.toList());
  }

  /**
   * Gets list of pen web blob metadata by submission number.
   *
   * @param submissionNumber the submission number
   * @return the list of pen web blob metadata by submission number
   */
  @Override
  public List<PENWebBlobMetadata> getPenWebBlobMetadata(final String submissionNumber) {
    final var blobEntities = this.getService().findPenWebBlobBySubmissionNumber(submissionNumber);
    return blobEntities.stream().map(penWebBlobMapper::toMetadataStructure).collect(Collectors.toList());
  }

  /**
   * Update pen web blob pen web blob.
   *
   * @param penWebBlob the pen web blob
   * @param sourceID   the pen web blob id
   * @return the pen web blob
   */
  @Override
  public PENWebBlob updatePenWebBlob(final PENWebBlob penWebBlob, final Long sourceID) {
    return penWebBlobMapper.toStructure(this.getService().updatePenWebBlob(penWebBlobMapper.toModel(penWebBlob), sourceID));
  }

  /**
   * Find all students completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @Override
  public CompletableFuture<Page<PenRequestBatchStudent>> findAllStudents(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification = null;
    try {
      final var associationNames = this.getSortCriteria(sortCriteriaJson, JsonUtil.mapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        final List<Search> searches = JsonUtil.mapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        this.getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (final var search : searches) {
          penRequestBatchStudentEntitySpecification = this.getSpecifications(penRequestBatchStudentEntitySpecification, i, search, associationNames, this.penRegBatchStudentFilterSpecs);
          i++;
        }

      }
    } catch (final JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }
    return this.getStudentService().findAll(penRequestBatchStudentEntitySpecification, pageNumber, pageSize, sorts).thenApplyAsync(penRegBatchEntities -> penRegBatchEntities.map(studentMapper::toStructure));
  }

  /**
   * Gets all pen request batch student status codes.
   *
   * @return the all pen request batch student status codes
   */
  @Override
  public List<PenRequestBatchStudentStatusCode> getAllPenRequestBatchStudentStatusCodes() {
    return this.getStudentService().getAllStudentStatusCodes().stream().map(studentStatusCodeMapper::toStruct).collect(Collectors.toList());
  }

  /**
   * Read pen request batch stats pen request batch stats.
   *
   * @return the pen request batch stats
   */
  @Override
  public PenRequestBatchStats readPenRequestBatchStats() {
    return this.getService().getStats();
  }

  /**
   * this is called from external clients to the ministry , so it is expected the intermediate backing api will validate the payload before passing it on here.
   *
   * @param penRequestBatchSubmission the pen request batch submission
   * @return response entity
   */
  @Override
  public ResponseEntity<UUID> createNewBatchSubmission(final PenRequestBatchSubmission penRequestBatchSubmission) {
    try {
      val existingSubmission = this.service.findPenRequestBatchBySubmissionNumber(penRequestBatchSubmission.getSubmissionNumber())
        .stream().anyMatch(penRequestBatchEntity -> PenRequestBatchProcessTypeCodes.API.getCode().equals(penRequestBatchEntity.getPenRequestBatchProcessTypeCode())
          && penRequestBatchSubmission.getFileType().equals(penRequestBatchEntity.getFileType()));
      if (existingSubmission) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      val batchEntity = mapper.toModel(penRequestBatchSubmission);
      this.populateAuditColumns(batchEntity);
      for (val reqStudent : penRequestBatchSubmission.getStudents()) {
        val studentEntity = studentMapper.toModel(reqStudent);
        studentEntity.setPenRequestBatchEntity(batchEntity);
        studentEntity.setCreateDate(batchEntity.getCreateDate());
        studentEntity.setUpdateDate(batchEntity.getUpdateDate());
        studentEntity.setCreateUser(batchEntity.getCreateUser());
        studentEntity.setUpdateUser(batchEntity.getUpdateUser());
        batchEntity.getPenRequestBatchStudentEntities().add(studentEntity);
      }

      return ResponseEntity.status(HttpStatus.CREATED).body(this.service.savePenRequestBatch(batchEntity).getPenRequestBatchID());
    } catch (final DataIntegrityViolationException e) {
      log.error("Integrity violation exception", e);
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

  }

  /**
   * Batch submission result response entity.
   *
   * @param batchSubmissionID the batch submission id this is pen request batch id.
   * @return the response entity
   */
  @Override
  public ResponseEntity<PenRequestBatchSubmissionResult> batchSubmissionResult(final UUID batchSubmissionID) {
    val penRequestBatch = this.service.getPenRequestBatchEntityByID(batchSubmissionID).orElseThrow(EntityNotFoundException::new);
    if (PenRegBatchHelper.isPRBStatusConsideredComplete(penRequestBatch.getPenRequestBatchStatusCode())) {
      try {
        return ResponseEntity.ok(batchResultMapper.toResult(penRequestBatch, this.service.populateStudentDataFromBatch(penRequestBatch)));
      } catch (final Exception e) {
        Thread.currentThread().interrupt();
        log.error("Exception ex :: ", e);
        throw new PenRegAPIRuntimeException(e.getMessage());
      }
    }
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }


  /**
   * Post pen request response entity.
   *
   * @param penRequest the pen request
   * @return the response entity
   */
  @Override
  public ResponseEntity<PenRequestResult> postPenRequest(final PenRequest penRequest) {
    val pair = this.service.postPenRequest(penRequest);
    if (pair.getValue().isPresent()) {
      return ResponseEntity.status(pair.getKey()).body(pair.getValue().get());
    }
    return ResponseEntity.status(pair.getKey()).build();
  }

  /**
   * Find all pen request i ds response entity.
   *
   * @param penRequestBatchIDs                the pen request batch i ds
   * @param penRequestBatchStudentStatusCodes the pen request batch student status codes
   * @param searchCriteriaListJson            the search criteria list json
   * @return the response entity
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public ResponseEntity<List<PenRequestIDs>> findAllPenRequestIDs(final List<UUID> penRequestBatchIDs, final List<String> penRequestBatchStudentStatusCodes, final String searchCriteriaListJson) throws JsonProcessingException {
    val errorCode = penRequestBatchStudentStatusCodes.stream().filter(statusCode -> PenRequestBatchStudentStatusCodes.valueOfCode(statusCode) == null).findFirst();
    if (errorCode.isPresent()) {
      log.error("Invalid pen request batch student status code provided :: " + errorCode);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    Map<String, String> searchCriteria = null;
    if (StringUtils.isNotBlank(searchCriteriaListJson)) {
      searchCriteria = JsonUtil.mapper.readValue(searchCriteriaListJson, new TypeReference<>() {
      });
    }
    return ResponseEntity.ok(this.getService().findAllPenRequestIDs(penRequestBatchIDs, penRequestBatchStudentStatusCodes, searchCriteria));
  }

  /**
   * Find all pen request batch validation issues by student id response entity.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the response entity
   */
  @Override
  public ResponseEntity<List<PenRequestBatchStudentValidationIssue>> findAllPenRequestBatchValidationIssuesByStudentID(final UUID penRequestBatchStudentID) {
    return ResponseEntity.ok(this.penRequestBatchStudentValidationIssueService.findAllValidationIssuesByPRBStudentID(penRequestBatchStudentID).stream().map(PenRequestBatchStudentValidationIssueMapper.mapper::toPenRequestBatchStruct).collect(Collectors.toList()));
  }


  @Override
  public ResponseEntity<List<PenRequestBatchStudentValidationIssue>> findAllPenRequestBatchValidationIssuesByStudentIDs(final List<UUID> penRequestBatchStudentIDs) {
    return ResponseEntity.ok(this.penRequestBatchStudentValidationIssueService.findAllValidationIssuesByPRBStudentIDs(penRequestBatchStudentIDs)
      .stream().map(PenRequestBatchStudentValidationIssueMapper.mapper::toPenRequestBatchStruct).collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<List<PenRequestBatch>> archiveBatchFiles(final List<PenRequestBatch> penRequestBatches) {
    val batchIds = penRequestBatches.stream().map(PenRequestBatch::getPenRequestBatchID).map(UUID::fromString).collect(Collectors.toList());
    val errorWithDupPenAssigned = this.sagaService.findDuplicatePenAssignedToDiffPRInSameBatchByBatchIds(batchIds);
    if (errorWithDupPenAssigned.isPresent()) {
      final ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(errorWithDupPenAssigned.get()).status(BAD_REQUEST).build();
      throw new InvalidPayloadException(error);
    }
    final var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(batchIds, this.getStatusesFilter()).isEmpty();
    if (sagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    val penRequestBatchesFromDB = this.getService().findAllByBatchIds(batchIds);
    if (penRequestBatchesFromDB.size() != batchIds.size()) {
      final ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Invalid Batch Id provided in the payload.").status(BAD_REQUEST).build();
      throw new InvalidPayloadException(error);
    }
    penRequestBatchesFromDB.forEach(el -> {
      el.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ARCHIVED.getCode());
      el.setProcessDate(LocalDateTime.now());
    });
    this.getService().updateAllPenRequestBatchAttachedEntities(penRequestBatchesFromDB);
    return ResponseEntity.ok(penRequestBatchesFromDB.stream().map(PenRequestBatchMapper.mapper::toStructure).collect(Collectors.toList()));
  }

  /**
   * Populate audit columns.
   *
   * @param model the model
   */
  private void populateAuditColumns(final PenRequestBatchEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
    model.setPenRequestBatchProcessTypeCode(PenRequestBatchProcessTypeCodes.API.getCode());
  }

  /**
   * Populate audit columns for student.
   *
   * @param model the model
   */
  private void populateAuditColumnsForStudent(final PenRequestBatchStudentEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
  }

  /**
   * Gets statuses filter.
   *
   * @return the statuses filter
   */
  protected List<String> getStatusesFilter() {
    final var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    return statuses;
  }

}
