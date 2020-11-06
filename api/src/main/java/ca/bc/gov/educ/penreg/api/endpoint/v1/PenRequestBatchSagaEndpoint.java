package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/pen-request-batch-saga")
public interface PenRequestBatchSagaEndpoint {

  @PostMapping("/new-pen")
  @PreAuthorize("#oauth2.hasAnyScope('PEN_REQUEST_BATCH_NEW_PEN_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start issue new pen saga.", description = "Endpoint to start issue new pen saga")
  @Schema(name = "PenRequestBatchNewPenSagaData", implementation = PenRequestBatchUserActionsSagaData.class)
  ResponseEntity<String> issueNewPen(@Validated @RequestBody PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData);

  @PostMapping("/user-match")
  @PreAuthorize("#oauth2.hasAnyScope('PEN_REQUEST_BATCH_USER_MATCH_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start User Match of a student to a pen request.",
      description = "Endpoint to start User Match of a student to a pen request")
  @Schema(name = "PenRequestBatchNewPenSagaData", implementation = PenRequestBatchUserActionsSagaData.class)
  ResponseEntity<String> processStudentRequestMatchedByUser(@Validated @RequestBody PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData);
}
