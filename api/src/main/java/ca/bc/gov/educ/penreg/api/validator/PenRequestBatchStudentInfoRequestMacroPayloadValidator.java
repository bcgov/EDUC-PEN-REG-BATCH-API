package ca.bc.gov.educ.penreg.api.validator;

import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentInfoRequestMacroService;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Component
public class PenRequestBatchStudentInfoRequestMacroPayloadValidator {
    @Getter(PRIVATE)
    private final PenRequestBatchStudentInfoRequestMacroService penRequestMacroService;

    public PenRequestBatchStudentInfoRequestMacroPayloadValidator(PenRequestBatchStudentInfoRequestMacroService penRequestMacroService) {
        this.penRequestMacroService = penRequestMacroService;
    }

    public List<FieldError> validatePayload(PenRequestBatchStudentInfoRequestMacro penRequestMacro, boolean isCreateOperation) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();
        if (isCreateOperation && penRequestMacro.getMacroId() != null) {
            apiValidationErrors.add(createFieldError("macroId", penRequestMacro.getMacroId(), "macroId should be null for post operation."));
        }
        return apiValidationErrors;
    }

    private FieldError createFieldError(String fieldName, Object rejectedValue, String message) {
        return new FieldError(PenRequestBatchStudentInfoRequestMacro.class.getName(), fieldName, rejectedValue, false, null, null, message);
    }
}
