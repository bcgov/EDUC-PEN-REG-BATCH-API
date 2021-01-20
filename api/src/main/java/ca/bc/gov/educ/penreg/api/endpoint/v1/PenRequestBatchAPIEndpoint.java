package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
  @Tag(name = "Endpoint to get Pen Request Batch Entity.", description = "Endpoint to get Pen Request Batch Entity By ID.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
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
  @Tag(name = "Endpoint to create Pen Request Batch Entity.", description = "Endpoint to create Pen Request Batch Entity.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
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
  @Tag(name = "Endpoint to update Pen Request Batch Entity.", description = "Endpoint to update the Pen Request Batch Entity")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  PenRequestBatch updatePenRequestBatch(@Validated @RequestBody PenRequestBatch penRequestBatch, @PathVariable UUID penRequestBatchID);


  /**
   * Find all completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search list , the JSON string ( of Array or List of {@link ca.bc.gov.educ.penreg.api.struct.v1.Search})
   * @return the completable future Page {@link PenRequestBatch}
   */
  @GetMapping("/paginated")
  @PreAuthorize("#oauth2.hasScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<PenRequestBatchSearch>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                   @ArraySchema(schema = @Schema(name = "searchCriteriaList",
                                                       description = "searchCriteriaList if provided should be a JSON string of Search Array",
                                                       implementation = ca.bc.gov.educ.penreg.api.struct.v1.Search.class))
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
  @Tag(name = "Endpoint to create Pen Request Batch Student Entity.", description = "Endpoint to create Pen Request Batch Student Entity.")
  @Schema(name = "PenRequestBatchStudent", implementation = PenRequestBatchStudent.class)
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
  @Tag(name = "Endpoint to update Pen Request Batch Student Entity.", description = "Endpoint to update Pen Request Batch Student Entity.")
  @Schema(name = "PenRequestBatchStudent", implementation = PenRequestBatchStudent.class)
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
  @Tag(name = "Endpoint to get Pen Request Batch Student Entity.", description = "Endpoint to get Pen Request Batch Student Entity by pen request batch student id and pen request batch id.")
  @Schema(name = "PenRequestBatchStudent", implementation = PenRequestBatchStudent.class)
  PenRequestBatchStudent getPenRequestBatchStudentByID(@PathVariable UUID penRequestBatchID, @PathVariable UUID penRequestBatchStudentID);

  /**
   * Gets pen request batch by submission number.
   *
   * @param submissionNumber the submission number
   * @return the pen request batch by submission number
   */
  @GetMapping
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get Pen Request Batch by submission number.", description = "Endpoint to get Pen Request Batch by submission number.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  PenRequestBatch getPenRequestBatchBySubmissionNumber(@RequestParam("submissionNumber") String submissionNumber);

  /**
   * Delete pen request batch response entity.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the response entity
   */
  @DeleteMapping("/{penRequestBatchID}")
  @PreAuthorize("#oauth2.hasAnyScope('DELETE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT."), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Endpoint to delete Pen Request Batch Entity.", description = "Endpoint to delete Pen Request Batch Entity By ID.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  ResponseEntity<Void> deletePenRequestBatch(@PathVariable UUID penRequestBatchID);

  /**
   * Gets pen web blob by submission number.
   *
   * @param submissionNumber the submission number
   * @return the pen web blob by submission number
   */
  @GetMapping("/source")
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_REQUEST_BATCH_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get source entity by Submission number.", description = "Endpoint to get source entity of Pen request batch by submission number.")
  @Schema(name = "PENWebBlob", implementation = PENWebBlob.class)
  PENWebBlob getPenWebBlobBySubmissionNumber(@RequestParam("submissionNumber") String submissionNumber);

  /**
   * Update pen web blob pen web blob.
   *
   * @param penWebBlob the pen web blob
   * @param sourceID   the source id
   * @return the pen web blob
   */
  @PutMapping("/source/{sourceID}")
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_PEN_REQUEST_BATCH_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Endpoint to update source entity by ID.", description = "Endpoint to update source entity by ID.")
  @Schema(name = "PENWebBlob", implementation = PENWebBlob.class)
  PENWebBlob updatePenWebBlob(@Validated @RequestBody PENWebBlob penWebBlob, @PathVariable Long sourceID);

  /**
   * Find all PenRequestBatchStudent for given search criteria.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search list , the JSON string ( of Array or List of {@link ca.bc.gov.educ.penreg.api.struct.v1.Search})
   * @return the completable future Page {@link PenRequestBatchStudent}
   */
  @GetMapping("/student/paginated")
  @PreAuthorize("#oauth2.hasScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination, for Student Requests.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<PenRequestBatchStudent>> findAllStudents(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                           @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                           @ArraySchema(schema = @Schema(name = "searchCriteriaList",
                                                               description = "searchCriteriaList if provided should be a JSON string of Search Array",
                                                               implementation = ca.bc.gov.educ.penreg.api.struct.v1.Search.class))
                                                           @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

  /**
   * Gets all pen request batch student status codes.
   *
   * @return the all pen request batch student status codes
   */
  @GetMapping("/student/pen-request-batch-student-status-codes")
  @PreAuthorize("#oauth2.hasAnyScope('READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get all the PenRequestBatchStudentStatusCode.")
  List<PenRequestBatchStudentStatusCode> getAllPenRequestBatchStudentStatusCodes();

}
