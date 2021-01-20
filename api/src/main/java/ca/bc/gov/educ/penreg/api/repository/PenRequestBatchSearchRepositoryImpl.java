package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class PenRequestBatchSearchRepositoryImpl implements PenRequestBatchSearchRepository {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<Tuple> findByAttributesAndPenRequestBatchStudent(Specification<PenRequestBatchEntity> specification, Pageable pageable) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    Long total = getTotalCount(cb, specification, pageable);

    List<Tuple> pageContent = List.of();
    if(total > 0) {
      CriteriaQuery<Tuple> query = cb.createTupleQuery();
      applyQueryCriteria(query, cb, specification, pageable, (batch, student) ->
        student != null ? query.multiselect(batch, cb.count(student)) : query.multiselect(batch));
      pageContent = entityManager.createQuery(query).setFirstResult((int)pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
    }

    return new PageImpl<>(pageContent, pageable, total);
  }

  public <T> void applyQueryCriteria(CriteriaQuery<T> query, CriteriaBuilder cb, Specification<PenRequestBatchEntity> specification, Pageable pageable, BiFunction<Root<?>, Join<?,?>, CriteriaQuery<T>> select) {
    Root<PenRequestBatchEntity> batch = query.from(PenRequestBatchEntity.class);

    var hasSpecification = specification != null;
    var predicate = hasSpecification ? specification.toPredicate(batch, query, cb) : null;

    var hasJoin = batch.getJoins().size() > 0;
    var student = hasJoin ? batch.getJoins().iterator().next() : null;

    var orders = pageable.getSort().stream().map(order -> getOrder(cb, order, batch, student)).collect(Collectors.toList());

    select.apply(batch, student)
      .orderBy(orders);

    if(hasSpecification) {
      query.where(predicate);
    }

    if(hasJoin) {
      query.groupBy(batch);
    }
  }

  private Long getTotalCount(CriteriaBuilder cb, Specification<PenRequestBatchEntity> specification, Pageable pageable) {
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    applyQueryCriteria(countQuery, cb, specification, pageable, (batch, student) -> countQuery.select(cb.count(batch)));

    Long total = 0L;
    try {
      total = entityManager.createQuery(countQuery).getSingleResult();
    } catch (NoResultException e) {
      log.debug("No Result");
    }

    return total;
  }

  private <Y> Path<Y> getFieldPath(String field, Root<?> root, Join<?, ?> join) {
    var names = field.split("\\.");
    return (names.length > 1 && join != null) ? join.<Y>get(names[1]) : root.<Y>get(field);
  }

  private Order getOrder(CriteriaBuilder cb, Sort.Order order, Root<?> root, Join<?, ?> join) {
    var field = order.getProperty();
    var path = getFieldPath(field, root, join);
    if(order.isAscending()) {
      return cb.asc(path);
    } else {
      return cb.desc(path);
    }
  }
}
