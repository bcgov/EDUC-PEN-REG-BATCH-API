package ca.bc.gov.educ.penreg.api.controller;

import ca.bc.gov.educ.penreg.api.endpoint.PenRegAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.penreg.api.exception.StudentRuntimeException;
import ca.bc.gov.educ.penreg.api.exception.errors.ApiError;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.filter.StudentFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.model.StudentEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.service.StudentService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.validator.StudentPayloadValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;


/**
 * Student controller
 *
 * @author John Cox
 */

@RestController
@EnableResourceServer
@Slf4j
public class PenRegAPIController implements PenRegAPIEndpoint {
  @Getter(AccessLevel.PRIVATE)
  private final StudentService service;

  @Getter(AccessLevel.PRIVATE)
  private final StudentPayloadValidator payloadValidator;
  private static final StudentMapper mapper = StudentMapper.mapper;
  private final StudentFilterSpecs studentFilterSpecs;

  @Autowired
  PenRegAPIController(final StudentService studentService, StudentPayloadValidator payloadValidator, StudentFilterSpecs studentFilterSpecs) {
    this.service = studentService;
    this.payloadValidator = payloadValidator;
    this.studentFilterSpecs = studentFilterSpecs;
  }

  public Student readStudent(String studentID) {
    return mapper.toStructure(getService().retrieveStudent(UUID.fromString(studentID)));
  }

  public Iterable<Student> findStudent(String pen) {
    Optional<StudentEntity> studentsResponse = getService().retrieveStudentByPen(pen);
    return studentsResponse.map(mapper::toStructure).map(Collections::singletonList).orElseGet(Collections::emptyList);
  }

  public Student createStudent(Student student) {
    validatePayload(student, true);
    setAuditColumns(student);
    return mapper.toStructure(getService().createStudent(mapper.toModel(student)));
  }

  public Student updateStudent(Student student) {
    validatePayload(student, false);
    setAuditColumns(student);
    return mapper.toStructure(getService().updateStudent(mapper.toModel(student)));
  }

  private void validatePayload(Student student, boolean isCreateOperation) {
    val validationResult = getPayloadValidator().validatePayload(student, isCreateOperation);
    if (!validationResult.isEmpty()) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }

  /**
   * set audit data to the object.
   *
   * @param student The object which will be persisted.
   */
  private void setAuditColumns(Student student) {
    if (StringUtils.isBlank(student.getCreateUser())) {
      student.setCreateUser(ApplicationProperties.STUDENT_API);
    }
    if (StringUtils.isBlank(student.getUpdateUser())) {
      student.setUpdateUser(ApplicationProperties.STUDENT_API);
    }
    student.setCreateDate(LocalDateTime.now().toString());
    student.setUpdateDate(LocalDateTime.now().toString());
  }

  public List<GenderCode> getGenderCodes() {
    return getService().getGenderCodesList().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  public List<SexCode> getSexCodes() {
    return getService().getSexCodesList().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  public List<DemogCode> getDemogCodes() {
    return getService().getDemogCodesList().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  public List<GradeCode> getGradeCodes() {
    return getService().getGradeCodesList().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  public List<StatusCode> getStatusCodes() {
    return getService().getStatusCodesList().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public ResponseEntity<Void> deleteById(final UUID id) {
    getService().deleteById(id);
    return ResponseEntity.noContent().build();
  }
  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<Student>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<StudentEntity> studentSpecs = null;
    try {
      getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<SearchCriteria> criteriaList = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        studentSpecs = getStudentEntitySpecification(criteriaList);
      }
    } catch (JsonProcessingException e) {
      throw new StudentRuntimeException(e.getMessage());
    }
    return getService().findAll(studentSpecs, pageNumber, pageSize, sorts).thenApplyAsync(studentEntities -> studentEntities.map(mapper::toStructure));
  }


  private void getSortCriteria(String sortCriteriaJson, ObjectMapper objectMapper, List<Sort.Order> sorts) throws JsonProcessingException {
    if (StringUtils.isNotBlank(sortCriteriaJson)) {
      Map<String, String> sortMap = objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
      });
      sortMap.forEach((k, v) -> {
        if ("ASC".equalsIgnoreCase(v)) {
          sorts.add(new Sort.Order(Sort.Direction.ASC, k));
        } else {
          sorts.add(new Sort.Order(Sort.Direction.DESC, k));
        }
      });
    }
  }

  private Specification<StudentEntity> getStudentEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<StudentEntity> studentSpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          Specification<StudentEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType());
          if (i == 0) {
            studentSpecs = Specification.where(typeSpecification);
          } else {
            assert studentSpecs != null;
            studentSpecs = studentSpecs.and(typeSpecification);
          }
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for", criteria.getKey(), criteria.getOperation().toString(), criteria.getValueType().toString());
        }
      }
    }
    return studentSpecs;
  }

  private Specification<StudentEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<StudentEntity> studentEntitySpecification = null;
    switch (valueType) {
      case STRING:
        studentEntitySpecification = studentFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        studentEntitySpecification = studentFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        studentEntitySpecification = studentFilterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        studentEntitySpecification = studentFilterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        studentEntitySpecification = studentFilterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        studentEntitySpecification = studentFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return studentEntitySpecification;
  }
  
}
