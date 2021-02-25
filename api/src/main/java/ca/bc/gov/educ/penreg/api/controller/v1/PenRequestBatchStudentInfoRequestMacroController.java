package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchStudentInfoRequestMacroEndpoint;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.penreg.api.exception.errors.ApiError;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentInfoRequestMacroMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentInfoRequestMacroEntity;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentInfoRequestMacroService;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import ca.bc.gov.educ.penreg.api.validator.PenRequestBatchStudentInfoRequestMacroPayloadValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@EnableResourceServer
@Slf4j
public class PenRequestBatchStudentInfoRequestMacroController implements PenRequestBatchStudentInfoRequestMacroEndpoint {

  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  private static final PenRequestBatchStudentInfoRequestMacroMapper mapper = PenRequestBatchStudentInfoRequestMacroMapper.mapper;
  @Getter(PRIVATE)
  private final PenRequestBatchStudentInfoRequestMacroService penRequestBatchMacroService;
  @Getter(PRIVATE)
  private final PenRequestBatchStudentInfoRequestMacroPayloadValidator penRequestBatchMacroPayloadValidator;

  @Autowired
  public PenRequestBatchStudentInfoRequestMacroController(final PenRequestBatchStudentInfoRequestMacroService penRequestMacroService, final PenRequestBatchStudentInfoRequestMacroPayloadValidator penRequestBatchMacroPayloadValidator) {
    this.penRequestBatchMacroService = penRequestMacroService;
    this.penRequestBatchMacroPayloadValidator = penRequestBatchMacroPayloadValidator;
  }

  @Override
  public List<PenRequestBatchStudentInfoRequestMacro> findPenReqMacros() {
    return this.getPenRequestBatchMacroService().findAllMacros().stream().map(mapper::toStructure).collect(Collectors.toList());
  }

  @Override
  public PenRequestBatchStudentInfoRequestMacro findPenReqMacroById(final UUID macroId) {
    val result = this.getPenRequestBatchMacroService().getMacro(macroId);
    if (result.isPresent()) {
      return mapper.toStructure(result.get());
    }
    throw new EntityNotFoundException(PenRequestBatchStudentInfoRequestMacro.class, "macroId", macroId.toString());
  }

  @Override
  public PenRequestBatchStudentInfoRequestMacro createPenReqMacro(final PenRequestBatchStudentInfoRequestMacro penRequestBatchMacro) {
    this.validatePayload(penRequestBatchMacro, true);
    final var model = mapper.toModel(penRequestBatchMacro);
    this.populateAuditColumns(model);
    return mapper.toStructure(this.getPenRequestBatchMacroService().createMacro(model));
  }

  @Override
  public PenRequestBatchStudentInfoRequestMacro updatePenReqMacro(final UUID macroId, final PenRequestBatchStudentInfoRequestMacro penRequestBatchMacro) {
    this.validatePayload(penRequestBatchMacro, false);
    final var model = mapper.toModel(penRequestBatchMacro);
    this.populateAuditColumns(model);
    return mapper.toStructure(this.getPenRequestBatchMacroService().updateMacro(macroId, model));
  }

  private void validatePayload(final PenRequestBatchStudentInfoRequestMacro penRequestMacro, final boolean isCreateOperation) {
    val validationResult = this.getPenRequestBatchMacroPayloadValidator().validatePayload(penRequestMacro, isCreateOperation);
    this.penRequestBatchMacroPayloadValidator.validatePayload(penRequestMacro, isCreateOperation);
    if (!validationResult.isEmpty()) {
      final ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }

  /**
   * Populate audit columns.
   *
   * @param model the model
   */
  private void populateAuditColumns(final PenRequestBatchStudentInfoRequestMacroEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
  }
}
