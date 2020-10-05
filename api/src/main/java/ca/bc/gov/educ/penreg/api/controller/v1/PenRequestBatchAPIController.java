package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchFilterSpecs;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchStudentFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenWebBlobMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.StudentStatusCodeMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentService;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;


/**
 * Student controller
 *
 * @author Om
 */
@RestController
@EnableResourceServer
@Slf4j
public class PenRequestBatchAPIController implements PenRequestBatchAPIEndpoint {

  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
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
   * The Event task scheduler async service.
   */
  private final EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;

  /**
   * Instantiates a new Pen request batch api controller.
   *
   * @param penRegBatchFilterSpecs         the pen reg batch filter specs
   * @param penRegBatchStudentFilterSpecs  the pen reg batch student filter specs
   * @param service                        the service
   * @param studentService                 the student service
   * @param eventTaskSchedulerAsyncService the event task scheduler async service
   */
  @Autowired
  public PenRequestBatchAPIController(final PenRegBatchFilterSpecs penRegBatchFilterSpecs, PenRegBatchStudentFilterSpecs penRegBatchStudentFilterSpecs, final PenRequestBatchService service, PenRequestBatchStudentService studentService, EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService) {
    this.penRegBatchFilterSpecs = penRegBatchFilterSpecs;
    this.penRegBatchStudentFilterSpecs = penRegBatchStudentFilterSpecs;
    this.service = service;
    this.studentService = studentService;
    this.eventTaskSchedulerAsyncService = eventTaskSchedulerAsyncService;
  }

  /**
   * Read pen request batch pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @Override
  public PenRequestBatch readPenRequestBatch(final UUID penRequestBatchID) {
    return getService().getPenRequestBatchEntityByID(penRequestBatchID).map(mapper::toStructure).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Create pen request batch pen request batch.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch
   */
  @Override
  public PenRequestBatch createPenRequestBatch(final PenRequestBatch penRequestBatch) {
    var model = mapper.toModel(penRequestBatch);
    populateAuditColumns(model);
    return mapper.toStructure(getService().createPenRequestBatch(model));
  }


  /**
   * Update pen request batch pen request batch.
   *
   * @param penRequestBatch   the pen request batch
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @Override
  public PenRequestBatch updatePenRequestBatch(final PenRequestBatch penRequestBatch, final UUID penRequestBatchID) {
    var model = mapper.toModel(penRequestBatch);
    populateAuditColumns(model);
    return mapper.toStructure(getService().createPenRequestBatch(model));
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
  public CompletableFuture<Page<PenRequestBatch>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchList) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchEntity> penRegBatchSpecs = null;
    try {
      getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchList)) {
        List<Search> searches = objectMapper.readValue(searchList, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          penRegBatchSpecs = getSpecifications(penRegBatchSpecs, i, search);
          i++;
        }

      }
    } catch (JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }
    return getService().findAll(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(penRegBatchEntities -> penRegBatchEntities.map(mapper::toStructure));
  }

  /**
   * Gets specifications.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param i                the
   * @param search           the search
   * @return the specifications
   */
  private Specification<PenRequestBatchEntity> getSpecifications(Specification<PenRequestBatchEntity> penRegBatchSpecs, int i, Search search) {
    if (i == 0) {
      penRegBatchSpecs = getPenRequestBatchEntitySpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        penRegBatchSpecs = penRegBatchSpecs.and(getPenRequestBatchEntitySpecification(search.getSearchCriteriaList()));
      } else {
        penRegBatchSpecs = penRegBatchSpecs.or(getPenRequestBatchEntitySpecification(search.getSearchCriteriaList()));
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
  public PenRequestBatchStudent createPenRequestBatchStudent(PenRequestBatchStudent penRequestBatchStudent, UUID penRequestBatchID) {
    var model = studentMapper.toModel(penRequestBatchStudent);
    populateAuditColumnsForStudent(model);
    return studentMapper.toStructure(getStudentService().createStudent(model, penRequestBatchID));
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
  public PenRequestBatchStudent updatePenRequestBatchStudent(PenRequestBatchStudent penRequestBatchStudent, UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    var model = studentMapper.toModel(penRequestBatchStudent);
    populateAuditColumnsForStudent(model);
    return studentMapper.toStructure(getStudentService().updateStudent(model, penRequestBatchID, penRequestBatchStudentID));
  }

  /**
   * Gets pen request batch student by id.
   *
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student by id
   */
  @Override
  public PenRequestBatchStudent getPenRequestBatchStudentByID(UUID penRequestBatchID, UUID penRequestBatchStudentID) {
    return studentMapper.toStructure(getStudentService().getStudentById(penRequestBatchID, penRequestBatchStudentID));
  }

  /**
   * Gets pen request batch by submission number.
   *
   * @param submissionNumber the submission number
   * @return the pen request batch by submission number
   */
  @Override
  public PenRequestBatch getPenRequestBatchBySubmissionNumber(String submissionNumber) {
    return getService().findPenRequestBatchBySubmissionNumber(submissionNumber).map(mapper::toStructure).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Delete pen request batch response entity.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the response entity
   */
  @Override
  public ResponseEntity<Void> deletePenRequestBatch(UUID penRequestBatchID) {
    getService().deletePenRequestBatch(penRequestBatchID);
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets pen web blob by submission number.
   *
   * @param submissionNumber the submission number
   * @return the pen web blob by submission number
   */
  @Override
  public PENWebBlob getPenWebBlobBySubmissionNumber(String submissionNumber) {
    return getService().findPenWebBlobBySubmissionNumber(submissionNumber).map(penWebBlobMapper::toStructure).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Update pen web blob pen web blob.
   *
   * @param penWebBlob   the pen web blob
   * @param penWebBlobId the pen web blob id
   * @return the pen web blob
   */
  @Override
  public PENWebBlob updatePenWebBlob(PENWebBlob penWebBlob, Long penWebBlobId) {
    return penWebBlobMapper.toStructure(getService().updatePenWebBlob(penWebBlobMapper.toModel(penWebBlob), penWebBlobId));
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
  public CompletableFuture<Page<PenRequestBatchStudent>> findAllStudents(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification = null;
    try {
      var associationNames = getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (var search : searches) {
          penRequestBatchStudentEntitySpecification = getStudentSpecifications(penRequestBatchStudentEntitySpecification, i, search, associationNames);
          i++;
        }

      }
    } catch (JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }
    return getStudentService().findAll(penRequestBatchStudentEntitySpecification, pageNumber, pageSize, sorts).thenApplyAsync(penRegBatchEntities -> penRegBatchEntities.map(studentMapper::toStructure));
  }

  /**
   * Get association names from search criterias, like penRequestBatchEntity.minCode
   *
   * @param associationNames the associations
   * @param searches         the search criterias
   */
  private void getAssociationNamesFromSearchCriterias(Associations associationNames, List<Search> searches) {
    searches.forEach(search -> search.getSearchCriteriaList().forEach(criteria -> {
      var names = criteria.getKey().split("\\.");
      if(names.length > 1) {
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
    return getStudentService().getAllStudentStatusCodes().stream().map(studentStatusCodeMapper::toStruct).collect(Collectors.toList());
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
  private Associations getSortCriteria(String sortCriteriaJson, ObjectMapper objectMapper, List<Sort.Order> sorts) throws JsonProcessingException {
    final Associations associationNames = new Associations();
    if (StringUtils.isNotBlank(sortCriteriaJson)) {
      Map<String, String> sortMap = objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
      });
      sortMap.forEach((k, v) -> {
        var names = k.split("\\.");
        if(names.length > 1) {
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
   * @param criteriaList the criteria list
   * @return the pen request batch entity specification
   */
  private Specification<PenRequestBatchEntity> getPenRequestBatchEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          Specification<PenRequestBatchEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType());
          penRequestBatchEntitySpecification = getSpecificationPerGroup(penRequestBatchEntitySpecification, i, criteria, typeSpecification);
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
  private Specification<PenRequestBatchEntity> getSpecificationPerGroup(Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification, int i, SearchCriteria criteria, Specification<PenRequestBatchEntity> typeSpecification) {
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
   * @param key             the key
   * @param filterOperation the filter operation
   * @param value           the value
   * @param valueType       the value type
   * @return the type specification
   */
  private Specification<PenRequestBatchEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification = null;
    Associations associationNames = new Associations();
    switch (valueType) {
      case STRING:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getStringTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE_TIME:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getDateTimeTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case LONG:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getLongTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case INTEGER:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getIntegerTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getDateTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case UUID:
        penRequestBatchEntitySpecification = getPenRegBatchFilterSpecs().getUUIDTypeSpecification(key, value, filterOperation, associationNames);
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
  private Specification<PenRequestBatchStudentEntity> getStudentTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType, Associations associationNames) {
    Specification<PenRequestBatchStudentEntity> penRequestBatchEntitySpecification = null;
    switch (valueType) {
      case STRING:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getStringTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE_TIME:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getDateTimeTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case LONG:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getLongTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case INTEGER:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getIntegerTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getDateTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case UUID:
        penRequestBatchEntitySpecification = getPenRegBatchStudentFilterSpecs().getUUIDTypeSpecification(key, value, filterOperation, associationNames);
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
  private Specification<PenRequestBatchStudentEntity> getStudentSpecifications(Specification<PenRequestBatchStudentEntity> studentEntitySpecification, int i, Search search, Associations associationNames) {
    if (i == 0) {
      studentEntitySpecification = getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames);
    } else {
      if (search.getCondition() == Condition.AND) {
        studentEntitySpecification = studentEntitySpecification.and(getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames));
      } else {
        studentEntitySpecification = studentEntitySpecification.or(getPenRequestBatchStudentEntitySpecification(search.getSearchCriteriaList(), associationNames));
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
  private Specification<PenRequestBatchStudentEntity> getPenRequestBatchStudentEntitySpecification(List<SearchCriteria> criteriaList, Associations associationNames) {
    Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          Specification<PenRequestBatchStudentEntity> typeSpecification = getStudentTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType(), associationNames);
          penRequestBatchStudentEntitySpecification = getSpecificationPerGroupOfStudentEntity(penRequestBatchStudentEntitySpecification, i, criteria, typeSpecification);
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
  private Specification<PenRequestBatchStudentEntity> getSpecificationPerGroupOfStudentEntity(Specification<PenRequestBatchStudentEntity> penRequestBatchStudentEntitySpecification, int i, SearchCriteria criteria, Specification<PenRequestBatchStudentEntity> typeSpecification) {
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
  private void populateAuditColumns(PenRequestBatchEntity model) {
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
   * Populate audit columns for student.
   *
   * @param model the model
   */
  private void populateAuditColumnsForStudent(PenRequestBatchStudentEntity model) {
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
   * Test.
   */
  public void test(){
    eventTaskSchedulerAsyncService.publishUnprocessedStudentRecords();
  }
}
