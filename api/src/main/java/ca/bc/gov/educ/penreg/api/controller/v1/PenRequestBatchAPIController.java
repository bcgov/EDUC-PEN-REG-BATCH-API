package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchFilterSpecs;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchStudentFilterSpecs;
import ca.bc.gov.educ.penreg.api.helpers.PenRegBatchHelper;
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
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStats;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmission;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmissionResult;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;


/**
 * Student controller
 *
 * @author Om
 */
@RestController
@Slf4j
public class PenRequestBatchAPIController implements PenRequestBatchAPIEndpoint {
  private final ObjectMapper objectMapper = new ObjectMapper();
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;

  private static final PenRequestBatchResultDataMapper batchResultMapper = PenRequestBatchResultDataMapper.mapper;
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
   * Instantiates a new Pen request batch api controller.
   *
   * @param penRegBatchFilterSpecs        the pen reg batch filter specs
   * @param penRegBatchStudentFilterSpecs the pen reg batch student filter specs
   * @param service                       the service
   * @param studentService                the student service
   */
  @Autowired
  public PenRequestBatchAPIController(final PenRegBatchFilterSpecs penRegBatchFilterSpecs,
                                      final PenRegBatchStudentFilterSpecs penRegBatchStudentFilterSpecs,
                                      final PenRequestBatchService service,
                                      final PenRequestBatchStudentService studentService,
                                      SagaService sagaService) {
    this.penRegBatchFilterSpecs = penRegBatchFilterSpecs;
    this.penRegBatchStudentFilterSpecs = penRegBatchStudentFilterSpecs;
    this.service = service;
    this.studentService = studentService;
    this.sagaService = sagaService;
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
    var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(List.of(penRequestBatchID), this.getStatusesFilter()).isEmpty();
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
      associationNames = this.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchList)) {
        final List<Search> searches = objectMapper.readValue(searchList, new TypeReference<>() {
        });
        this.getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (final var search : searches) {
          penRegBatchSpecs = this.getSpecifications(penRegBatchSpecs, i, search, associationNames);
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
   * Gets specifications.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param i                the
   * @param search           the search
   * @param associationNames the association names
   * @return the specifications
   */
  private Specification<PenRequestBatchEntity> getSpecifications(Specification<PenRequestBatchEntity> penRegBatchSpecs, final int i, final Search search, final Associations associationNames) {
    if (i == 0) {
      penRegBatchSpecs = this.getPenRequestBatchEntitySpecification(search.getSearchCriteriaList(), associationNames);
    } else {
      if (search.getCondition() == Condition.AND) {
        penRegBatchSpecs = penRegBatchSpecs.and(this.getPenRequestBatchEntitySpecification(search.getSearchCriteriaList(), associationNames));
      } else {
        penRegBatchSpecs = penRegBatchSpecs.or(this.getPenRequestBatchEntitySpecification(search.getSearchCriteriaList(), associationNames));
      }
    }
    return penRegBatchSpecs;
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
   * @param penWebBlob   the pen web blob
   * @param penWebBlobId the pen web blob id
   * @return the pen web blob
   */
  @Override
  public PENWebBlob updatePenWebBlob(final PENWebBlob penWebBlob, final Long penWebBlobId) {
    return penWebBlobMapper.toStructure(this.getService().updatePenWebBlob(penWebBlobMapper.toModel(penWebBlob), penWebBlobId));
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
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification = null;
    try {
      final var associationNames = this.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        final List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        this.getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (final var search : searches) {
          penRequestBatchStudentEntitySpecification = this.getStudentSpecifications(penRequestBatchStudentEntitySpecification, i, search, associationNames);
          i++;
        }

      }
    } catch (final JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }
    return this.getStudentService().findAll(penRequestBatchStudentEntitySpecification, pageNumber, pageSize, sorts).thenApplyAsync(penRegBatchEntities -> penRegBatchEntities.map(studentMapper::toStructure));
  }

  /**
   * Get association names from search criterias, like penRequestBatchEntity.mincode
   *
   * @param associationNames the associations
   * @param searches         the search criterias
   */
  private void getAssociationNamesFromSearchCriterias(final Associations associationNames, final List<Search> searches) {
    searches.forEach(search -> search.getSearchCriteriaList().forEach(criteria -> {
      final var names = criteria.getKey().split("\\.");
      if (names.length > 1) {
        associationNames.getSortAssociations().remove(names[0]);
        associationNames.getSearchAssociations().add(names[0]);
      }
    }));
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


  @Override
  public ResponseEntity<PenRequestResult> postPenRequest(final PenRequest penRequest) {
    val pair = this.service.postPenRequest(penRequest);
    if (pair.getValue().isPresent()) {
      return ResponseEntity.status(pair.getKey()).body(pair.getValue().get());
    }
    return ResponseEntity.status(pair.getKey()).build();
  }

  @Override
  public List<PenRequestIDs> findAllPenRequestIDs(List<UUID> penRequestBatchIDs, List<PenRequestBatchStudentStatusCodes> penRequestBatchStudentStatusCodes) {
    return this.getService().findAllPenRequestIDs(penRequestBatchIDs, penRequestBatchStudentStatusCodes);
  }

  /**
   * Gets sort criteria.
   *
   * @param sortCriteriaJson the sort criteria json
   * @param objectMapper     the object mapper
   * @param sorts            the sorts
   * @return the association names
   * @throws JsonProcessingException the json processing exception
   */
  private Associations getSortCriteria(final String sortCriteriaJson, final ObjectMapper objectMapper, final List<Sort.Order> sorts) throws JsonProcessingException {
    final Associations associationNames = new Associations();
    if (StringUtils.isNotBlank(sortCriteriaJson)) {
      final Map<String, String> sortMap = objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
      });
      sortMap.forEach((k, v) -> {
        final var names = k.split("\\.");
        if (names.length > 1) {
          associationNames.getSortAssociations().add(names[0]);
        }

        if ("ASC".equalsIgnoreCase(v)) {
          sorts.add(new Sort.Order(Sort.Direction.ASC, k));
        } else {
          sorts.add(new Sort.Order(Sort.Direction.DESC, k));
        }
      });
    }
    return associationNames;
  }

  /**
   * Gets pen request batch entity specification.
   *
   * @param criteriaList     the criteria list
   * @param associationNames the association names
   * @return the pen request batch entity specification
   */
  private Specification<PenRequestBatchEntity> getPenRequestBatchEntitySpecification(final List<SearchCriteria> criteriaList, final Associations associationNames) {
    Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (final SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          final Specification<PenRequestBatchEntity> typeSpecification = this.getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType(), associationNames);
          penRequestBatchEntitySpecification = this.getSpecificationPerGroup(penRequestBatchEntitySpecification, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for", criteria.getKey(), criteria.getOperation().toString(), criteria.getValueType().toString());
        }
      }
    }
    return penRequestBatchEntitySpecification;
  }

  /**
   * Gets specification per group.
   *
   * @param penRequestBatchEntitySpecification the pen request batch entity specification
   * @param i                                  the
   * @param criteria                           the criteria
   * @param typeSpecification                  the type specification
   * @return the specification per group
   */
  private Specification<PenRequestBatchEntity> getSpecificationPerGroup(Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification, final int i, final SearchCriteria criteria, final Specification<PenRequestBatchEntity> typeSpecification) {
    if (i == 0) {
      penRequestBatchEntitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        penRequestBatchEntitySpecification = penRequestBatchEntitySpecification.and(typeSpecification);
      } else {
        penRequestBatchEntitySpecification = penRequestBatchEntitySpecification.or(typeSpecification);
      }
    }
    return penRequestBatchEntitySpecification;
  }

  /**
   * Gets type specification.
   *
   * @param key              the key
   * @param filterOperation  the filter operation
   * @param value            the value
   * @param valueType        the value type
   * @param associationNames the association names
   * @return the type specification
   */
  private Specification<PenRequestBatchEntity> getTypeSpecification(final String key, final FilterOperation filterOperation, final String value, final ValueType valueType, final Associations associationNames) {
    Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification = null;
    switch (valueType) {
      case STRING:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getStringTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE_TIME:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getDateTimeTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case LONG:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getLongTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case INTEGER:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getIntegerTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getDateTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case UUID:
        penRequestBatchEntitySpecification = this.getPenRegBatchFilterSpecs().getUUIDTypeSpecification(key, value, filterOperation, associationNames);
        break;
      default:
        break;
    }
    return penRequestBatchEntitySpecification;
  }

  /**
   * Gets student type specification.
   *
   * @param key              the key
   * @param filterOperation  the filter operation
   * @param value            the value
   * @param valueType        the value type
   * @param associationNames the association names
   * @return the student type specification
   */
  private Specification<PenRequestBatchStudentEntity> getStudentTypeSpecification(final String key, final FilterOperation filterOperation, final String value, final ValueType valueType, final Associations associationNames) {
    Specification<PenRequestBatchStudentEntity> penRequestBatchEntitySpecification = null;
    switch (valueType) {
      case STRING:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getStringTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE_TIME:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getDateTimeTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case LONG:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getLongTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case INTEGER:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getIntegerTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getDateTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case UUID:
        penRequestBatchEntitySpecification = this.getPenRegBatchStudentFilterSpecs().getUUIDTypeSpecification(key, value, filterOperation, associationNames);
        break;
      default:
        break;
    }
    return penRequestBatchEntitySpecification;
  }


  /**
   * Gets student specifications.
   *
   * @param studentEntitySpecification the student entity specification
   * @param i                          the
   * @param search                     the search
   * @param associationNames           the association names
   * @return the student specifications
   */
  private Specification<PenRequestBatchStudentEntity> getStudentSpecifications(Specification<PenRequestBatchStudentEntity> studentEntitySpecification, final int i, final Search search, final Associations associationNames) {
    if (i == 0) {
      studentEntitySpecification = this.getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames);
    } else {
      if (search.getCondition() == Condition.AND) {
        studentEntitySpecification = studentEntitySpecification.and(this.getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames));
      } else {
        studentEntitySpecification = studentEntitySpecification.or(this.getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames));
      }
    }
    return studentEntitySpecification;
  }

  /**
   * Gets pen request batch student entity specification.
   *
   * @param criteriaList     the criteria list
   * @param associationNames the association names
   * @return the pen request batch student entity specification
   */
  private Specification<PenRequestBatchStudentEntity> getPenRequestBatchStudentEntitySpecification(final List<SearchCriteria> criteriaList, final Associations associationNames) {
    Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (final SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          final Specification<PenRequestBatchStudentEntity> typeSpecification = this.getStudentTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType(), associationNames);
          penRequestBatchStudentEntitySpecification = this.getSpecificationPerGroupOfStudentEntity(penRequestBatchStudentEntitySpecification, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for", criteria.getKey(), criteria.getOperation().toString(), criteria.getValueType().toString());
        }
      }
    }
    return penRequestBatchStudentEntitySpecification;
  }

  /**
   * Gets specification per group of student entity.
   *
   * @param penRequestBatchStudentEntitySpecification the pen request batch student entity specification
   * @param i                                         the
   * @param criteria                                  the criteria
   * @param typeSpecification                         the type specification
   * @return the specification per group of student entity
   */
  private Specification<PenRequestBatchStudentEntity> getSpecificationPerGroupOfStudentEntity(Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification, final int i, final SearchCriteria criteria, final Specification<PenRequestBatchStudentEntity> typeSpecification) {
    if (i == 0) {
      penRequestBatchStudentEntitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        penRequestBatchStudentEntitySpecification = penRequestBatchStudentEntitySpecification.and(typeSpecification);
      } else {
        penRequestBatchStudentEntitySpecification = penRequestBatchStudentEntitySpecification.or(typeSpecification);
      }
    }
    return penRequestBatchStudentEntitySpecification;
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

  protected List<String> getStatusesFilter() {
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    return statuses;
  }

}
