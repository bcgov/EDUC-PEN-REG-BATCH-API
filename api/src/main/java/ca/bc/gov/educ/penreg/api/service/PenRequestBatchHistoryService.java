package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen reg batch service.
 *
 * @author OM
 */
@Service
@Slf4j
public class PenRequestBatchHistoryService {

  /**
   * The Repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchHistoryRepository repository;

  /**
   * Instantiates a new Pen reg batch service.
   *
   * @param repository           the repository
   */
  @Autowired
  public PenRequestBatchHistoryService(final PenRequestBatchHistoryRepository repository) {
    this.repository = repository;
  }

  /**
   * Find all completable future.
   *
   * @param penRegBatchSpecs the pen reg batch specs
   * @param pageNumber       the page number
   * @param pageSize         the page size
   * @param sorts            the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<PenRequestBatchHistoryEntity>> findAll(final Specification<PenRequestBatchHistoryEntity> penRegBatchSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.getRepository().findAll(penRegBatchSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }
}
