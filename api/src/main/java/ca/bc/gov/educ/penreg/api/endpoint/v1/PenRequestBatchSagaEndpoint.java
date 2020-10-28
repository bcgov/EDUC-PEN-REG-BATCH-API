package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchNewPenSagaData;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/pen-request-batch-saga")
public interface PenRequestBatchSagaEndpoint {
    @PostMapping("/new-pen")
    @PreAuthorize("#oauth2.hasAnyScope('PEN_REQUEST_BATCH_NEW_PEN_SAGA')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
    @Transactional
    @Tag(name = "Endpoint to start issue new pen saga.", description = "Endpoint to start issue new pen saga")
    @Schema(name = "PenRequestBatchNewPenSagaData", implementation = PenRequestBatchNewPenSagaData.class)
    ResponseEntity<String> issueNewPen(@Validated @RequestBody PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData);
}
