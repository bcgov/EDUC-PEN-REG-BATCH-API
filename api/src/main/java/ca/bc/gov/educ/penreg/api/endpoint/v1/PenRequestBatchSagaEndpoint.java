package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequestMapping("/api/v1/pen-request-batch-saga")
public interface PenRequestBatchSagaEndpoint {

  @GetMapping("/{sagaID}")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "Not Found.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to retrieve saga by its ID (GUID).", description = "Endpoint to retrieve saga by its ID (GUID).")
  ResponseEntity<Saga> readSaga(@PathVariable UUID sagaID);


  @PostMapping("/new-pen")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_NEW_PEN_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start issue new pen saga.", description = "Endpoint to start issue new pen saga")
  @Schema(name = "PenRequestBatchUserActionsSagaData", implementation = PenRequestBatchUserActionsSagaData.class)
  ResponseEntity<String> issueNewPen(@Validated @RequestBody PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData);

  @PostMapping("/user-match")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_USER_MATCH_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start User Match of a student to a pen request.",
      description = "Endpoint to start User Match of a student to a pen request")
  @Schema(name = "PenRequestBatchUserActionsSagaData", implementation = PenRequestBatchUserActionsSagaData.class)
  ResponseEntity<String> processStudentRequestMatchedByUser(@Validated @RequestBody PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData);

  @PostMapping("/user-unmatch")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_USER_MATCH_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start User Unmatch of a student to a pen request.",
    description = "Endpoint to start User Unmatch of a student to a pen request")
  @Schema(name = "PenRequestBatchUnmatchSagaData", implementation = PenRequestBatchUnmatchSagaData.class)
  ResponseEntity<String> processStudentRequestUnmatchedByUser(@Validated @RequestBody PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData);

  @PostMapping("/archive-and-return")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_ARCHIVE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start archive and return files saga.", description = "archive and return files saga")
  @Schema(name = "PenRequestBatchArchiveAndReturnSagaData", implementation = PenRequestBatchArchiveAndReturnAllSagaData.class)
  ResponseEntity<List<ArchiveAndReturnSagaResponse>> archiveAndReturnAllFiles(@Validated @RequestBody PenRequestBatchArchiveAndReturnAllSagaData penRequestBatchArchiveAndReturnAllSagaData);

  @PostMapping("/repost-reports")
  @PreAuthorize("hasAuthority('SCOPE_PEN_REQUEST_BATCH_REPOST_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start repost reports saga.", description = "repost reports saga")
  @Schema(name = "PenRequestBatchReturnFilesSagaData", implementation = PenRequestBatchRepostReportsFilesSagaData.class)
  ResponseEntity<String> repostReports(@Validated @RequestBody PenRequestBatchRepostReportsFilesSagaData penRequestBatchRepostReportsSagaData);

  /**
   * Find all Sagas for given search criteria.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search list , the JSON string ( of Array or List of {@link ca.bc.gov.educ.penreg.api.struct.v1.Search})
   * @return the completable future Page {@link Saga}
   */
  @GetMapping("/paginated")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination, for Sagas.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<Saga>> findAllSagas(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                  @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                                  @ArraySchema(schema = @Schema(name = "searchCriteriaList",
                                                                    description = "searchCriteriaList if provided should be a JSON string of Search Array",
                                                                    implementation = ca.bc.gov.educ.penreg.api.struct.v1.Search.class))
                                                                  @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);
}
