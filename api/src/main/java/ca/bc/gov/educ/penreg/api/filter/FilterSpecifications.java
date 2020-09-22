package ca.bc.gov.educ.penreg.api.filter;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.*;
import java.util.EnumMap;
import java.util.function.BiFunction;

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

	private Path<T> getFieldValue(FilterCriteria<T> filterCriteria, Root<E> root, CriteriaQuery criteriaQuery,Associations associationNames) {

  	//fetch all associations in the orderBy statement, execute fetch only once for one association
		associationNames.getSortAssociations().forEach(association -> root.fetch(association, JoinType.INNER));
		associationNames.getSortAssociations().clear();

		var names = filterCriteria.getFieldName().split("\\.");
		//fetch the association in the where condition
		if(names.length > 1 && associationNames.getSearchAssociations().contains(names[0])) {
			Join<Object, Object> join = associationNames.countJoin(names[0]);
			if(join == null) {
				//Hibernate may run a count query to determine the number of results, and this can cause the 'the owner of the fetched association was not present in the select list' error.
				//To avoid this error by checking the return type of the query before using the fetch.
				if (criteriaQuery.getResultType().equals(Long.class)) {
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

		map.put(FilterOperation.CONTAINS, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> {
			return criteriaBuilder
					.like((Expression<String>) getFieldValue(filterCriteria, root, criteriaQuery, associationNames), "%" + filterCriteria.getConvertedSingleValue() + "%");
		});

		map.put(FilterOperation.CONTAINS_IGNORE_CASE, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
				.like(criteriaBuilder.lower((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames)), "%" + filterCriteria.getConvertedSingleValue().toString().toLowerCase() + "%"));

    map.put(FilterOperation.STARTS_WITH, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .like((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames), filterCriteria.getConvertedSingleValue() + "%"));

    map.put(FilterOperation.STARTS_WITH_IGNORE_CASE, (filterCriteria, associationNames) -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .like(criteriaBuilder.lower((Expression<String>)getFieldValue(filterCriteria, root, criteriaQuery, associationNames)), filterCriteria.getConvertedSingleValue().toString().toLowerCase() + "%"));

	}
}
