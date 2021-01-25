package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class PenRequestBatchSearchRepositoryImpl implements PenRequestBatchSearchRepository {
  public static final String PEN_REQUEST_BATCH_ID = "penRequestBatchID";
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<Pair<PenRequestBatchEntity, Long>> findByPenRequestBatchStudent(@NonNull Specification<PenRequestBatchEntity> specification, Pageable pageable) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    List<Tuple> studentCounts = getPenRequestBatchIDsAndStudentCounts(specification, pageable, cb);
    List<Pair<PenRequestBatchEntity, Long>> pageContent = List.of();
    if(studentCounts.size() > 0) {
      Map<UUID, PenRequestBatchEntity> batchMap = getPenRequestBatchEntities(cb, studentCounts);
      pageContent = studentCounts.stream().map((tuple) -> Pair.of(batchMap.get(tuple.get(0, UUID.class)), tuple.get(1, Long.class))).collect(Collectors.toList());
    }

    long total = (pageable.getPageNumber() > 0 || studentCounts.size() == pageable.getPageSize()) ? getTotalCount(cb, specification, pageable) : studentCounts.size();

    return new PageImpl<>(pageContent, pageable, total);
  }

  private List<Tuple> getPenRequestBatchIDsAndStudentCounts(@NonNull Specification<PenRequestBatchEntity> specification, Pageable pageable, CriteriaBuilder cb) {
    CriteriaQuery<Tuple> query = cb.createTupleQuery();

    Root<PenRequestBatchEntity> batch = query.from(PenRequestBatchEntity.class);

    var predicate = specification.toPredicate(batch, query, cb);

    if(batch.getJoins().size() == 0) {
      throw new IllegalArgumentException("An entity association should be provided in the specification");
    }

    var student = batch.getJoins().iterator().next();
    var orders = pageable.getSort().stream().map(order -> getOrder(cb, order, batch, student)).collect(Collectors.toList());
    List<Expression<?>> groupByExpressions = pageable.getSort().stream().map(order -> getOrderFieldPath(order, batch, student)).collect(Collectors.toList());
    groupByExpressions.add(0, batch);

    query.multiselect(batch.get(PEN_REQUEST_BATCH_ID), cb.count(batch.get(PEN_REQUEST_BATCH_ID)))
      .where(predicate)
      .groupBy(groupByExpressions)
      .orderBy(orders);

    return entityManager.createQuery(query).setFirstResult((int)pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
  }

  private Map<UUID, PenRequestBatchEntity> getPenRequestBatchEntities(CriteriaBuilder cb, List<Tuple> studentCounts) {
    var batchIDs = studentCounts.stream().map((tuple) -> tuple.get(0, UUID.class)).collect(Collectors.toList());
    CriteriaQuery<PenRequestBatchEntity> batchQuery = cb.createQuery(PenRequestBatchEntity.class);
    Root<PenRequestBatchEntity> batch = batchQuery.from(PenRequestBatchEntity.class);
    batchQuery.select(batch).where(batch.get(PEN_REQUEST_BATCH_ID).in(batchIDs));
    return entityManager.createQuery(batchQuery).getResultList().stream().collect(Collectors.toMap(PenRequestBatchEntity::getPenRequestBatchID, Function.identity()));
  }

  private Long getTotalCount(CriteriaBuilder cb, Specification<PenRequestBatchEntity> specification, Pageable pageable) {
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

    Root<PenRequestBatchEntity> batch = countQuery.from(PenRequestBatchEntity.class);

    //use sub-query to get result count
    Subquery<UUID> subquery = countQuery.subquery(UUID.class);
    Root<PenRequestBatchEntity> sqBatch = subquery.correlate(batch);

    var predicate = specification.toPredicate(sqBatch, countQuery, cb);

    subquery.select(sqBatch.get(PEN_REQUEST_BATCH_ID))
      .where(predicate)
      .groupBy(sqBatch);

    countQuery.select(cb.count(batch)).where(cb.exists(subquery));

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
    Path<Object> path = getOrderFieldPath(order, root, join);
    if(order.isAscending()) {
      return cb.asc(path);
    } else {
      return cb.desc(path);
    }
  }

  private Path<Object> getOrderFieldPath(Sort.Order order, Root<?> root, Join<?, ?> join) {
    var field = order.getProperty();
    return getFieldPath(field, root, join);
  }
}
