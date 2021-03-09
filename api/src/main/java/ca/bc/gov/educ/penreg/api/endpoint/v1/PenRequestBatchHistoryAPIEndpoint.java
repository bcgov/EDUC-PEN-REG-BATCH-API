package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchHistorySearch;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.CompletableFuture;

/**
 * The interface Pen reg api endpoint.
 */
@RequestMapping("/api/v1/pen-request-batch/history")
public interface PenRequestBatchHistoryAPIEndpoint {

  /**
   * Find all completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search list , the JSON string ( of Array or List of {@link Search})
   * @return the completable future Page {@link PenRequestBatch}
   */
  @GetMapping("/paginated")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH_HISTORY')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<PenRequestBatchHistorySearch>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                         @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                         @ArraySchema(schema = @Schema(name = "searchCriteriaList",
                                                                 description = "searchCriteriaList if provided should be a JSON string of Search Array",
                                                                 implementation = Search.class))
                                                         @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

}
