package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.BaseFilterSpecs;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.struct.v1.Condition;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import ca.bc.gov.educ.penreg.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.v1.ValueType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public class PaginatedController {
  /**
   * Gets sort criteria.
   *
   * @param sortCriteriaJson the sort criteria json
   * @param objectMapper     the object mapper
   * @param sorts            the sorts
   * @return the association names
   * @throws JsonProcessingException the json processing exception
   */
  public Associations getSortCriteria(final String sortCriteriaJson, final ObjectMapper objectMapper, final List<Sort.Order> sorts) throws JsonProcessingException {
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
  public void getAssociationNamesFromSearchCriterias(final Associations associationNames, final List<Search> searches) {
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
   * @param specs            specs
   * @param i                the
   * @param search           the search
   * @return the specifications
   */
  public <T> Specification<T> getSpecifications(Specification<T> specs, final int i, final Search search, final Associations associationNames, BaseFilterSpecs<T> filterSpecs) {
    if (i == 0) {
      specs = this.getEntitySpecification(search.getSearchCriteriaList(), associationNames, filterSpecs);
    } else {
      if (search.getCondition() == Condition.AND) {
        specs = specs.and(this.getEntitySpecification(search.getSearchCriteriaList(), associationNames, filterSpecs));
      } else {
        specs = specs.or(this.getEntitySpecification(search.getSearchCriteriaList(), associationNames, filterSpecs));
      }
    }
    return specs;
  }

  /**
   * Gets entity specification.
   *
   * @param criteriaList the criteria list
   * @return the entity specification
   */
  private <T> Specification<T> getEntitySpecification(final List<SearchCriteria> criteriaList, final Associations associationNames, BaseFilterSpecs<T> filterSpecs) {
    Specification<T> entitySpecification = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (final SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          final Specification<T> typeSpecification = this.getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType(), associationNames, filterSpecs);
          entitySpecification = this.getSpecificationPerGroup(entitySpecification, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for", criteria.getKey(), criteria.getOperation().toString(), criteria.getValueType().toString());
        }
      }
    }
    return entitySpecification;
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
  private <T> Specification<T> getTypeSpecification(final String key, final FilterOperation filterOperation, final String value, final ValueType valueType, final Associations associationNames, BaseFilterSpecs<T> filterSpecs) {
    Specification<T> entitySpecification = null;
    switch (valueType) {
      case STRING:
        entitySpecification = filterSpecs.getStringTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE_TIME:
        entitySpecification = filterSpecs.getDateTimeTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case LONG:
        entitySpecification = filterSpecs.getLongTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case INTEGER:
        entitySpecification = filterSpecs.getIntegerTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case DATE:
        entitySpecification = filterSpecs.getDateTypeSpecification(key, value, filterOperation, associationNames);
        break;
      case UUID:
        entitySpecification = filterSpecs.getUUIDTypeSpecification(key, value, filterOperation, associationNames);
        break;
      default:
        break;
    }
    return entitySpecification;
  }

  /**
   * Gets specification per group.
   *
   * @param entitySpecification                the entity specification
   * @param i                                  the
   * @param criteria                           the criteria
   * @param typeSpecification                  the type specification
   * @return the specification per group
   */
  private <T> Specification<T> getSpecificationPerGroup(Specification<T> entitySpecification, final int i, final SearchCriteria criteria, final Specification<T> typeSpecification) {
    if (i == 0) {
      entitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        entitySpecification = entitySpecification.and(typeSpecification);
      } else {
        entitySpecification = entitySpecification.or(typeSpecification);
      }
    }
    return entitySpecification;
  }
}


