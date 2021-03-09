package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchHistoryAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchHistoryFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchHistoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static lombok.AccessLevel.PRIVATE;


/**
 * Student controller
 *
 * @author Om
 */
@RestController
@Slf4j
public class PenRequestBatchHistoryAPIController implements PenRequestBatchHistoryAPIEndpoint {

  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  /**
   * The Pen reg batch filter specs.
   */
  @Getter(PRIVATE)
  private final PenRegBatchHistoryFilterSpecs penRegBatchFilterSpecs;
  /**
   * The Service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchHistoryService service;
  /**
   * The constant mapper.
   */
  private static final PenRequestBatchHistoryMapper mapper = PenRequestBatchHistoryMapper.mapper;

  /**
   * Instantiates a new Pen request batch api controller.
   *
   * @param penRegBatchFilterSpecs        the pen reg batch filter specs
   * @param service                       the service
   */
  @Autowired
  public PenRequestBatchHistoryAPIController(final PenRegBatchHistoryFilterSpecs penRegBatchFilterSpecs, final PenRequestBatchHistoryService service) {
    this.penRegBatchFilterSpecs = penRegBatchFilterSpecs;
    this.service = service;
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
  public CompletableFuture<Page<PenRequestBatchHistorySearch>> findAll(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchList) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<PenRequestBatchHistoryEntity> penRegBatchSpecs = null;
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

    return this.getService().findAll(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(page ->
          page.map(mapper::toSearchStructure));
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
   * Gets specifications.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param i                the
   * @param search           the search
   * @return the specifications
   */
  private Specification<PenRequestBatchHistoryEntity> getSpecifications(Specification<PenRequestBatchHistoryEntity> penRegBatchSpecs, final int i, final Search search, final Associations associationNames) {
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
   * Gets pen request batch entity specification.
   *
   * @param criteriaList the criteria list
   * @return the pen request batch entity specification
   */
  private Specification<PenRequestBatchHistoryEntity> getPenRequestBatchEntitySpecification(final List<SearchCriteria> criteriaList, final Associations associationNames) {
    Specification<PenRequestBatchHistoryEntity> penRequestBatchEntitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (final SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          final Specification<PenRequestBatchHistoryEntity> typeSpecification = this.getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType(), associationNames);
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
   * Gets type specification.
   *
   * @param key             the key
   * @param filterOperation the filter operation
   * @param value           the value
   * @param valueType       the value type
   * @return the type specification
   */
  private Specification<PenRequestBatchHistoryEntity> getTypeSpecification(final String key, final FilterOperation filterOperation, final String value, final ValueType valueType, final Associations associationNames) {
    Specification<PenRequestBatchHistoryEntity> penRequestBatchEntitySpecification = null;
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
   * Gets specification per group.
   *
   * @param penRequestBatchEntitySpecification the pen request batch entity specification
   * @param i                                  the
   * @param criteria                           the criteria
   * @param typeSpecification                  the type specification
   * @return the specification per group
   */
  private Specification<PenRequestBatchHistoryEntity> getSpecificationPerGroup(Specification<PenRequestBatchHistoryEntity> penRequestBatchEntitySpecification, final int i, final SearchCriteria criteria, final Specification<PenRequestBatchHistoryEntity> typeSpecification) {
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
}
