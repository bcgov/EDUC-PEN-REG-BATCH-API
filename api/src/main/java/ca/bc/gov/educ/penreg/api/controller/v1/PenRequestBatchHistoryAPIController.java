package ca.bc.gov.educ.penreg.api.controller.v1;

import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchHistoryAPIEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.filter.Associations;
import ca.bc.gov.educ.penreg.api.filter.PenRegBatchHistoryFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchHistoryService;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchHistorySearch;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RestController;


/**
 * Student controller
 *
 * @author Om
 */
@RestController
@Slf4j
public class PenRequestBatchHistoryAPIController extends PaginatedController implements PenRequestBatchHistoryAPIEndpoint {

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
          penRegBatchSpecs = this.getSpecifications(penRegBatchSpecs, i, search, associationNames, this.getPenRegBatchFilterSpecs());
          i++;
        }

      }
    } catch (final JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }

    return this.getService().findAll(penRegBatchSpecs, pageNumber, pageSize, sorts).thenApplyAsync(page ->
          page.map(mapper::toSearchStructure));
  }
}
