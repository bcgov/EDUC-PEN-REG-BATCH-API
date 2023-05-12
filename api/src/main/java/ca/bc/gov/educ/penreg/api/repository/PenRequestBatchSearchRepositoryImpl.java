package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class PenRequestBatchSearchRepositoryImpl implements PenRequestBatchSearchRepository {
  public static final String PEN_REQUEST_BATCH_ID = "penRequestBatchID";
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<Pair<PenRequestBatchEntity, Long>> findByPenRequestBatchStudent(@NonNull final Specification<PenRequestBatchEntity> specification, final Pageable pageable) {
    final CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();

    final List<Tuple> studentCounts = this.getPenRequestBatchIDsAndStudentCounts(specification, pageable, cb);
    List<Pair<PenRequestBatchEntity, Long>> pageContent = List.of();
    if (studentCounts.size() > 0) {
      final Map<UUID, PenRequestBatchEntity> batchMap = this.getPenRequestBatchEntities(cb, studentCounts);
      pageContent = studentCounts.stream().map((tuple) -> Pair.of(batchMap.get(tuple.get(0, UUID.class)), tuple.get(1, Long.class))).collect(Collectors.toList());
    }

    final long total = (pageable.getPageNumber() > 0 || studentCounts.size() == pageable.getPageSize()) ? this.getTotalCount(cb, specification, pageable) : studentCounts.size();

    return new PageImpl<>(pageContent, pageable, total);
  }

  private List<Tuple> getPenRequestBatchIDsAndStudentCounts(@NonNull final Specification<PenRequestBatchEntity> specification, final Pageable pageable, final CriteriaBuilder cb) {
    final CriteriaQuery<Tuple> query = cb.createTupleQuery();

    final Root<PenRequestBatchEntity> batch = query.from(PenRequestBatchEntity.class);

    final var predicate = specification.toPredicate(batch, query, cb);

    if (batch.getJoins().size() == 0) {
      throw new IllegalArgumentException("An entity association should be provided in the specification");
    }

    final var student = batch.getJoins().iterator().next();
    final var orders = pageable.getSort().stream().map(order -> this.getOrder(cb, order, batch, student)).collect(Collectors.toList());
    final List<Expression<?>> groupByExpressions = pageable.getSort().stream().map(order -> this.getOrderFieldPath(order, batch, student)).collect(Collectors.toList());
    groupByExpressions.add(0, batch);

    query.multiselect(batch.get(PEN_REQUEST_BATCH_ID), cb.count(batch.get(PEN_REQUEST_BATCH_ID)))
        .where(predicate)
        .groupBy(groupByExpressions)
        .orderBy(orders);

    return this.entityManager.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
  }

  private Map<UUID, PenRequestBatchEntity> getPenRequestBatchEntities(final CriteriaBuilder cb, final List<Tuple> studentCounts) {
    final var batchIDs = studentCounts.stream().map((tuple) -> tuple.get(0, UUID.class)).collect(Collectors.toList());
    final CriteriaQuery<PenRequestBatchEntity> batchQuery = cb.createQuery(PenRequestBatchEntity.class);
    final Root<PenRequestBatchEntity> batch = batchQuery.from(PenRequestBatchEntity.class);
    batchQuery.select(batch).where(batch.get(PEN_REQUEST_BATCH_ID).in(batchIDs));
    return this.entityManager.createQuery(batchQuery).getResultList().stream().collect(Collectors.toMap(PenRequestBatchEntity::getPenRequestBatchID, Function.identity()));
  }

  private Long getTotalCount(final CriteriaBuilder cb, final Specification<PenRequestBatchEntity> specification, final Pageable pageable) {
    final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

    final Root<PenRequestBatchEntity> batch = countQuery.from(PenRequestBatchEntity.class);

    //use sub-query to get result count
    final Subquery<UUID> subquery = countQuery.subquery(UUID.class);
    final Root<PenRequestBatchEntity> sqBatch = subquery.correlate(batch);

    final var predicate = specification.toPredicate(sqBatch, countQuery, cb);

    subquery.select(sqBatch.get(PEN_REQUEST_BATCH_ID))
        .where(predicate)
        .groupBy(sqBatch);

    countQuery.select(cb.count(batch)).where(cb.exists(subquery));

    Long total = 0L;
    try {
      total = this.entityManager.createQuery(countQuery).getSingleResult();
    } catch (final NoResultException e) {
      log.debug("No Result");
    }

    return total;
  }

  private <Y> Path<Y> getFieldPath(final String field, final Root<?> root, final Join<?, ?> join) {
    final var names = field.split("\\.");
    return (names.length > 1 && join != null) ? join.<Y>get(names[1]) : root.<Y>get(field);
  }

  private Order getOrder(final CriteriaBuilder cb, final Sort.Order order, final Root<?> root, final Join<?, ?> join) {
    final Path<Object> path = this.getOrderFieldPath(order, root, join);
    if (order.isAscending()) {
      return cb.asc(path);
    } else {
      return cb.desc(path);
    }
  }

  private Path<Object> getOrderFieldPath(final Sort.Order order, final Root<?> root, final Join<?, ?> join) {
    final var field = order.getProperty();
    return this.getFieldPath(field, root, join);
  }
}
