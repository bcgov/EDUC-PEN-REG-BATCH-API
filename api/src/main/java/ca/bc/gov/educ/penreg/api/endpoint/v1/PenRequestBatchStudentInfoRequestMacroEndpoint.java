package ca.bc.gov.educ.penreg.api.endpoint.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RequestMapping("/api/v1/pen-request-batch-macro")
public interface PenRequestBatchStudentInfoRequestMacroEndpoint {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH_MACRO')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<PenRequestBatchStudentInfoRequestMacro> findPenReqMacros();

    @GetMapping("/{macroId}")
    @PreAuthorize("hasAuthority('SCOPE_READ_PEN_REQUEST_BATCH_MACRO')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    PenRequestBatchStudentInfoRequestMacro findPenReqMacroById(@PathVariable UUID macroId);

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH_MACRO')")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED")})
    @ResponseStatus(CREATED)
    PenRequestBatchStudentInfoRequestMacro createPenReqMacro(@Validated @RequestBody PenRequestBatchStudentInfoRequestMacro penRequestMacro);

    @PutMapping("/{macroId}")
    @PreAuthorize("hasAuthority('SCOPE_WRITE_PEN_REQUEST_BATCH_MACRO')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    PenRequestBatchStudentInfoRequestMacro updatePenReqMacro(@PathVariable UUID macroId, @Validated @RequestBody PenRequestBatchStudentInfoRequestMacro penRequestMacro);
}
