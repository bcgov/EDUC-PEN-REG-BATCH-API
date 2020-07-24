package ca.bc.gov.educ.penreg.api.controller;

import ca.bc.gov.educ.penreg.api.endpoint.PenRequestBatchAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.service.PenRegBatchService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.ValueType;
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
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

  @Getter(PRIVATE)
  private final PenRegBatchFilterSpecs penRegBatchFilterSpecs;
  @Getter(PRIVATE)
  private final PenRegBatchService service;
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  @Autowired
  public PenRequestBatchAPIController(final PenRegBatchFilterSpecs penRegBatchFilterSpecs, final PenRegBatchService service) {
    this.penRegBatchFilterSpecs = penRegBatchFilterSpecs;
    this.service = service;
  }

  @Override
  public PenRequestBatch readPenRequestBatch(final UUID penRequestBatchID) {
    return getService().getPenRequestBatchEntityByID(penRequestBatchID).map(mapper::toStructure).orElseThrow(EntityNotFoundException::new);
  }

  @Override
  public PenRequestBatch createPenRequestBatch(final PenRequestBatch penRequestBatch) {
    return mapper.toStructure(getService().createPenRequestBatch(mapper.toModel(penRequestBatch)));
  }

  @Override
  public PenRequestBatch updatePenRequestBatch(final PenRequestBatch penRequestBatch, final UUID penRequestBatchID) {
    return mapper.toStructure(getService().createPenRequestBatch(mapper.toModel(penRequestBatch)));
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<PenRequestBatch>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchEntity> penRegBatchSpecs = null;
    try {
      getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<SearchCriteria> criteriaList = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        penRegBatchSpecs = getStudentEntitySpecification(criteriaList);
      }
    } catch (JsonProcessingException e) {
      throw new PenRegAPIRuntimeException(e.getMessage());
    }
    return getService().findAll(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(penRegBatchEntities -> penRegBatchEntities.map(mapper::toStructure));
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

  private Specification<PenRequestBatchEntity> getStudentEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<PenRequestBatchEntity> studentSpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          Specification<PenRequestBatchEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType());
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

  private Specification<PenRequestBatchEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<PenRequestBatchEntity> penRequestBatchEntitySpecification = null;
    switch (valueType) {
      case STRING:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        penRequestBatchEntitySpecification = penRegBatchFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return penRequestBatchEntitySpecification;
  }
  
}
