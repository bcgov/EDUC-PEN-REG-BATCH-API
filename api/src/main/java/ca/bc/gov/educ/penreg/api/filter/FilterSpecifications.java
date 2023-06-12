package ca.bc.gov.educ.penreg.api.filter;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.*;
import java.util.EnumMap;
import java.util.function.BiFunction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * The type Filter specifications.
 *
 * @param <E> the type parameter
 * @param <T> the type parameter
 */
@Service
public class FilterSpecifications<E, T extends Comparable<T>> {

  /**
   * The Map.
   */
  private EnumMap<FilterOperation, BiFunction<FilterCriteria<T>, Associations, Specification<E>>> map;

  /**
   * Instantiates a new Filter specifications.
   */
  public FilterSpecifications() {
		initSpecifications();
	}

  /**
   * Gets specification.
   *
   * @param operation the operation
   * @return the specification
   */
  public BiFunction<FilterCriteria<T>, Associations, Specification<E>> getSpecification(FilterOperation operation) {
		return map.get(operation);
	}

  /**
   * Gets field value.
   *
   * @param filterCriteria   the filter criteria
   * @param root             the root
   * @param criteriaQuery    the criteria query
   * @param associationNames the association names
   * @return the field value
   */
  private Path<T> getFieldValue(FilterCriteria<T> filterCriteria, Root<E> root, CriteriaQuery criteriaQuery,Associations associationNames) {

  	//fetch all associations in the orderBy statement, execute fetch only once for one association
		associationNames.getSortAssociations().forEach(association -> {
			if (isOneToManyRelation(association)) {
				root.join(association, JoinType.INNER);
			} else {
				root.fetch(association, JoinType.INNER);
			}
		});
		associationNames.getSortAssociations().clear();

		var names = filterCriteria.getFieldName().split("\\.");
		//fetch the association in the where condition
		if(names.length > 1 && associationNames.getSearchAssociations().contains(names[0])) {
			Join<Object, Object> join = associationNames.countJoin(names[0]);
			if(join == null) {
				//Hibernate may run a count query to determine the number of results, and this can cause the 'the owner of the fetched association was not present in the select list' error.
				//To avoid this error by checking the return type of the query before using the fetch.
				if (criteriaQuery.getResultType().equals(Long.class) || isOneToManyRelation(names[0])) {
					join = root.join(names[0], JoinType.INNER);
				} else {
					join = (Join<Object, Object>) root.fetch(names[0], JoinType.INNER);
				}
				associationNames.cacheJoin(names[0], join);
			}
			associationNames.resetIfAllJoinsProcessed();
			return join.get(names[1]);
		}

		return root.get(filterCriteria.getFieldName());
	}

	private boolean isOneToManyRelation(String association) {
		return association.endsWith("s");
	}

  /**
   * Init specifications.
   */
  @PostConstruct
	public void initSpecifications() {

		map = new EnumMap<>(FilterOperation.class);

		// Equal
		map.put(FilterOperation.EQUAL, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.equal(getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.NOT_EQUAL, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.notEqual(getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.GREATER_THAN,
			(filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThan(
						getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.GREATER_THAN_OR_EQUAL_TO,
			(filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
						getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.LESS_THAN, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.lessThan(getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.LESS_THAN_OR_EQUAL_TO,
			(filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(
						getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue()));

		map.put(FilterOperation.IN, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) ->
			getFieldValue(filterCriteria, root, criteriaQuery, associationNames).in(filterCriteria.getConvertedValues()));

		map.put(FilterOperation.NOT_IN, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.not(getFieldValue(filterCriteria, root, criteriaQuery, associationNames).in(filterCriteria.getConvertedSingleValue())));

		map.put(FilterOperation.BETWEEN,
			(filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(
						getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getMinValue(),
						filterCriteria.getMaxValue()));

		map.put(FilterOperation.CONTAINS, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .like((Expression<String>) getFieldValue(filterCriteria, root, criteriaQuery, associationNames), "%" + filterCriteria.getConvertedSingleValue() + "%"));

		map.put(FilterOperation.CONTAINS_IGNORE_CASE, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.like(criteriaBuilder.lower((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames)), "%" + filterCriteria.getConvertedSingleValue().toString().toLowerCase() + "%"));

    map.put(FilterOperation.STARTS_WITH, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .like((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue() + "%"));

    map.put(FilterOperation.STARTS_WITH_IGNORE_CASE, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .like(criteriaBuilder.lower((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames)), filterCriteria.getConvertedSingleValue().toString().toLowerCase() + "%"));

	}
}
