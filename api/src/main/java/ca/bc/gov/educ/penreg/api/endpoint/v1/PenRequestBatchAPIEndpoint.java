package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.PENWebBlob;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * The interface Pen reg api endpoint.
 */
@RequestMapping("/api/v1/pen-request-batch")
@OpenAPIDefinition(info = @Info(title = "API for Pen Registry.", description = "This CRU API is related to batch processing of student data.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_STUDENT", "WRITE_STUDENT"})})
public interface PenRequestBatchAPIEndpoint {


  /**
   * Read pen request batch pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @GetMapping(value = "/{penRequestBatchID}")
  @PreAuthorize("#oauth2.hasScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  PenRequestBatch readPenRequestBatch(@PathVariable UUID penRequestBatchID);


  /**
   * Create pen request batch pen request batch.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch
   */
  @PostMapping
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @ResponseStatus(CREATED)
  @Transactional
  PenRequestBatch createPenRequestBatch(@Validated @RequestBody PenRequestBatch penRequestBatch);


  /**
   * Update pen request batch pen request batch.
   *
   * @param penRequestBatch   the pen request batch
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @PutMapping("/{penRequestBatchID}")
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  PenRequestBatch updatePenRequestBatch(@Validated @RequestBody PenRequestBatch penRequestBatch, @PathVariable UUID penRequestBatchID);


  /**
   * Find all completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @GetMapping("/paginated")
  @Async
  @PreAuthorize("#oauth2.hasScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  CompletableFuture<Page<PenRequestBatch>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                   @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);


  /**
   * Create pen request batch student pen request batch student.
   *
   * @param penRequestBatchStudent the pen request batch student
   * @param penRequestBatchID      the pen request batch id
   * @return the pen request batch student
   */
  @PostMapping("/{penRequestBatchID}/student")
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @ResponseStatus(CREATED)
  @Transactional
  PenRequestBatchStudent createPenRequestBatchStudent(@Validated @RequestBody PenRequestBatchStudent penRequestBatchStudent, @PathVariable UUID penRequestBatchID);


  /**
   * Update pen request batch student pen request batch student.
   *
   * @param penRequestBatchStudent   the pen request batch student
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student
   */
  @PutMapping("/{penRequestBatchID}/student/{penRequestBatchStudentID}")
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Transactional
  PenRequestBatchStudent updatePenRequestBatchStudent(@Validated @RequestBody PenRequestBatchStudent penRequestBatchStudent, @PathVariable UUID penRequestBatchID, @PathVariable UUID penRequestBatchStudentID);


  /**
   * Gets pen request batch student by id.
   *
   * @param penRequestBatchID        the pen request batch id
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the pen request batch student by id
   */
  @GetMapping("/{penRequestBatchID}/student/{penRequestBatchStudentID}")
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Transactional(readOnly = true)
  PenRequestBatchStudent getPenRequestBatchStudentByID(@PathVariable UUID penRequestBatchID, @PathVariable UUID penRequestBatchStudentID);

  @GetMapping
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  PenRequestBatch getPenRequestBatchBySubmissionNumber(@RequestParam("submissionNumber") String submissionNumber);

  @DeleteMapping("/{penRequestBatchID}")
  @PreAuthorize("#oauth2.hasAnyScope('DELETE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT."), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  ResponseEntity<Void> deletePenRequestBatch(@PathVariable UUID penRequestBatchID);

  @GetMapping("/tsw-penweb-blobs")
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_WEB_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  PENWebBlob getPenWebBlobBySubmissionNumber(@RequestParam("submissionNumber") String submissionNumber);

  @PutMapping("/tsw-penweb-blobs/{penWebBlobId}")
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_WEB_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  PENWebBlob updatePenWebBlob(@Validated @RequestBody PENWebBlob penWebBlob, @PathVariable Long penWebBlobId);

}
