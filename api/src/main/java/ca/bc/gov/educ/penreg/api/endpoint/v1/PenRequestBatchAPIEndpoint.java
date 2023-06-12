package ca.bc.gov.educ.penreg.api.endpoint.v1;

import static org.springframework.http.HttpStatus.CREATED;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStats;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmission;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmissionResult;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The interface Pen reg api endpoint.
 */
@RequestMapping("/api/v1/pen-request-batch")
@OpenAPIDefinition(info = @Info(title = "API for Pen Registry.", description = "This CRU API is related to batch processing of student data.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_PEN_REQUEST_BATCH", "WRITE_PEN_REQUEST_BATCH, READ_PEN_REQUEST_BATCH_HISTORY"})})
public interface PenRequestBatchAPIEndpoint {


  /**
   * Read pen request batch pen request batch.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the pen request batch
   */
  @GetMapping(value = "/{penRequestBatchID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get Pen Request Batch Entity.", description = "Endpoint to get Pen Request Batch Entity By ID.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  PenRequestBatch readPenRequestBatch(@PathVariable UUID penRequestBatchID);

  /**
   * Find the same pen numbers issued to multiple students within a list of batches.
   *
   * @param penRequestBatchID the pen request batch id
   * @return list of Pen Request Batch Student Ids
   */
  @GetMapping(value = "same-pen")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get Pen Request Batch Student Ids with same assigned PEN", description = "Endpoint to get list of Pen Request Batch Student Ids that have the same assigned PEN number in one or more batches. Accepts comma separated list of Batch IDs")
  @Schema(name = "String", implementation = String.class)
  List<String> findAllSamePensWithinPenRequestBatchByID(@RequestParam(name = "penRequestBatchID") String penRequestBatchID);

  /**
   * Create pen request batch pen request batch.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch
   */
  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Endpoint to update Pen Request Batch Entity.", description = "Endpoint to update the Pen Request Batch Entity")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  ResponseEntity<PenRequestBatch> updatePenRequestBatch(@Validated @RequestBody PenRequestBatch penRequestBatch, @PathVariable UUID penRequestBatchID);


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
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get List of  Pen Request Batch by submission number.", description = "Endpoint to get List of  Pen Request Batch by submission number.")
  @ArraySchema(schema = @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class))
  List<PenRequestBatch> getPenRequestBatchBySubmissionNumber(@RequestParam("submissionNumber") String submissionNumber);

  /**
   * Delete pen request batch response entity.
   *
   * @param penRequestBatchID the pen request batch id
   * @return the response entity
   */
  @DeleteMapping("/{penRequestBatchID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT."), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Endpoint to delete Pen Request Batch Entity.", description = "Endpoint to delete Pen Request Batch Entity By ID.")
  @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class)
  ResponseEntity<Void> deletePenRequestBatch(@PathVariable UUID penRequestBatchID);

  /**
   * Gets list of pen web blob by submission number and file type.
   *
   * @param submissionNumber the submission number
   * @param fileType         the file type
   * @return the list of pen web blob by submission number and file type
   */
  @GetMapping("/source")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get source entity list by Submission number and file type.", description = "Endpoint to get source entity list of Pen request batch by submission number and file type.")
  @ArraySchema(schema = @Schema(name = "PENWebBlob", implementation = PENWebBlob.class))
  List<PENWebBlob> getPenWebBlobs(@RequestParam("submissionNumber") String submissionNumber, @RequestParam(name = "fileType", required = false) String fileType);

  /**
   * Gets list of pen web blob metadata by submission number.
   *
   * @param submissionNumber the submission number
   * @return the list of pen web blob metadata by submission number
   */
  @GetMapping("/source-metadata")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH_BLOB')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get source metadata list by Submission number.", description = "Endpoint to get source metadata list of Pen request batch by submission number.")
  @ArraySchema(schema = @Schema(name = "PENWebBlob", implementation = PENWebBlob.class))
  List<PENWebBlobMetadata> getPenWebBlobMetadata(@RequestParam("submissionNumber") String submissionNumber);

  /**
   * Update pen web blob pen web blob.
   *
   * @param penWebBlob the pen web blob
   * @param sourceID   the source id
   * @return the pen web blob
   */
  @PutMapping("/source/{sourceID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH_BLOB')")
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
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
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
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get all the PenRequestBatchStudentStatusCode.")
  List<PenRequestBatchStudentStatusCode> getAllPenRequestBatchStudentStatusCodes();

  /**
   * Read pen request batch stats list.
   *
   * @return the list of PenRequestBatchStats
   */
  @GetMapping(value = "/stats")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get the List of stats of Pen Request Batch.", description = "Endpoint to get the List of stats of Pen Request Batch.")
  @Schema(name = "PenRequestBatchStats", implementation = PenRequestBatchStats.class)
  PenRequestBatchStats readPenRequestBatchStats();


  /**
   * Create new batch submission response entity.
   *
   * @param penRequestBatchSubmission the pen request batch submission
   * @return the response entity
   */
  @PostMapping("/pen-request-batch-submission")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "409", description = "CONFLICT")})
  @ResponseStatus(CREATED)
  @Transactional
  @Tag(name = "Endpoint to create Pen Request Batch Submission, used for external clients to the ministry.",
    description = "This endpoint will allow external client to submit a batch request via api call. If the api call was success it will return a guid {batchSubmissionID} for further tracking")
  @Schema(name = "PenRequestBatchSubmission", implementation = PenRequestBatchSubmission.class)
  ResponseEntity<UUID> createNewBatchSubmission(@RequestBody PenRequestBatchSubmission penRequestBatchSubmission);

  /**
   * Batch submission result response entity.
   *
   * @param batchSubmissionID the batch submission id
   * @return the response entity
   */
  @GetMapping("/pen-request-batch-submission/{batchSubmissionID}/result")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "202", description = "ACCEPTED"), @ApiResponse(responseCode = "404", description =
    "NOT FOUND")})
  @Transactional
  @Tag(name = "Endpoint to get Pen Request Batch Submission results, used for external clients to the ministry.",
    description = "This endpoint will allow external client to query the results of a batch earlier submitted.")
  @Schema(name = "PenRequestBatchSubmissionResult", implementation = PenRequestBatchSubmissionResult.class)
  ResponseEntity<PenRequestBatchSubmissionResult> batchSubmissionResult(@PathVariable UUID batchSubmissionID);

  /**
   * this endpoint will help external clients like MyEd to request for a PEN.
   * 200 - Means Either Direct match to a student, matched student pen was returned Or validation errors were returned.
   * 201 - Means a new pen was created and pen was returned.
   * 300 - Means multiple choice found.
   *
   * @param penRequest the payload with student details
   * @return the response entity
   */
  @PostMapping("/pen-request")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "300", description = "MultipleChoices.")})
  @Transactional(propagation = Propagation.NEVER)
  @Tag(name = "Endpoint to provide one of pen request facility for external clients to the ministry.",
    description = "This endpoint will allow external client to request for a pen.")
  @Schema(name = "penRequest", implementation = PenRequest.class)
  ResponseEntity<PenRequestResult> postPenRequest(@Validated @RequestBody PenRequest penRequest);

  /**
   * Find all pen request ids given list of batch ids and status codes
   *
   * @param penRequestBatchIDs                the list of batch ids
   * @param penRequestBatchStudentStatusCodes the list of status codes
   * @param searchCriteria                    the search criteria
   * @return the list of PenRequestIDs {@link PenRequestIDs}
   * @throws JsonProcessingException the json processing exception
   */
  @GetMapping("/pen-request-batch-ids")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support pen request navigation view in frontend.", description = "This API endpoint exposes flexible way to query ids without returning the entire entity.")
  ResponseEntity<List<PenRequestIDs>> findAllPenRequestIDs(@RequestParam(name = "penRequestBatchIDs") List<UUID> penRequestBatchIDs,
                                                           @RequestParam(name = "penRequestBatchStudentStatusCodes") List<String> penRequestBatchStudentStatusCodes,
                                                           @ArraySchema(schema = @Schema(name = "searchCriteria",
                                                             description = "searchCriteria if provided should be a JSON string Map<String,String>",
                                                             implementation = java.util.Map.class))
                                                           @RequestParam(name = "searchCriteria", required = false) String searchCriteria) throws JsonProcessingException;

  /**
   * Find all pen request batch validation issues by student id response entity.
   *
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the response entity
   */
  @GetMapping("/students/{penRequestBatchStudentID}/validation-issues")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to retrieve the validation issues of a request student.", description = "Endpoint to retrieve the validation issues of a request student.")
  ResponseEntity<List<PenRequestBatchStudentValidationIssue>> findAllPenRequestBatchValidationIssuesByStudentID(@PathVariable UUID penRequestBatchStudentID);

  /**
   * Find all pen request batch validation issues by student ids response entity.
   *
   * @param penRequestBatchStudentIDs the pen request batch student ids
   * @return the response entity
   */
  @GetMapping("/students/validation-issues")
  @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to retrieve the validation issues of request students.", description = "Endpoint to retrieve the validation issues of request students.")
  ResponseEntity<List<PenRequestBatchStudentValidationIssue>> findAllPenRequestBatchValidationIssuesByStudentIDs(@RequestParam(name = "penRequestBatchStudentIDs") List<UUID> penRequestBatchStudentIDs);


  @PostMapping("/archive")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Transactional
  @Tag(name = "Endpoint to archive multiple batch files in a single transaction.", description = "Endpoint to archive multiple batch files in a single transaction")
  @ArraySchema(schema = @Schema(name = "PenRequestBatch", implementation = PenRequestBatch.class))
  ResponseEntity<List<PenRequestBatch>> archiveBatchFiles(@Validated @RequestBody List<PenRequestBatch> penRequestBatches);
}

