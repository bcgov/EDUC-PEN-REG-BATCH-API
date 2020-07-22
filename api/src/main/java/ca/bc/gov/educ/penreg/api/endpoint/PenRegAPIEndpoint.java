package ca.bc.gov.educ.penreg.api.endpoint;

import ca.bc.gov.educ.penreg.api.struct.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.CREATED;

@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for Pen Registry.", description = "This CRU API is related to batch processing of student data.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_STUDENT", "WRITE_STUDENT"})})
public interface PenRegAPIEndpoint {

  @GetMapping("/{studentID}")
  @PreAuthorize("#oauth2.hasScope('READ_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  Student readStudent(@PathVariable String studentID);
  
  @GetMapping
  @PreAuthorize("#oauth2.hasScope('READ_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  Iterable<Student> findStudent(@Param("pen")  String pen);

  @PostMapping
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @ResponseStatus(CREATED)
  Student createStudent(@Validated @RequestBody Student student);

  @PutMapping
  @PreAuthorize("#oauth2.hasAnyScope('WRITE_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  Student updateStudent(@Validated @RequestBody Student student);
  
  @PreAuthorize("#oauth2.hasScope('READ_STUDENT_CODES')")
  @GetMapping("sex-codes")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  List<SexCode> getSexCodes();

  @PreAuthorize("#oauth2.hasScope('READ_STUDENT_CODES')")
  @GetMapping("status-codes")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  List<StatusCode> getStatusCodes();

  @PreAuthorize("#oauth2.hasScope('READ_STUDENT_CODES')")
  @GetMapping("demog-codes")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  List<DemogCode> getDemogCodes();

  @PreAuthorize("#oauth2.hasScope('READ_STUDENT_CODES')")
  @GetMapping("grade-codes")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  List<GradeCode> getGradeCodes();

  @PreAuthorize("#oauth2.hasScope('READ_STUDENT_CODES')")
  @GetMapping("gender-codes")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  List<GenderCode> getGenderCodes();

  @DeleteMapping("/{id}")
  @PreAuthorize("#oauth2.hasScope('DELETE_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"),  @ApiResponse(responseCode = "404", description = "NOT FOUND."), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  ResponseEntity<Void> deleteById(@PathVariable UUID id);

  @GetMapping("/paginated")
  @Async
  @PreAuthorize("#oauth2.hasScope('READ_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  CompletableFuture<Page<Student>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                           @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                           @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);
}
