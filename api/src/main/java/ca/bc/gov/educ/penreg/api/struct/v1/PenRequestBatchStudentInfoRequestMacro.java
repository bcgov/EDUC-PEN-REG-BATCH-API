package ca.bc.gov.educ.penreg.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestBatchStudentInfoRequestMacro extends BaseRequest{
    private String macroId;

    @NotNull(message = "macroCode cannot be null")
    @Size(max = 10)
    private String macroCode;

    @NotNull(message = "macroText cannot be null")
    @Size(max = 4000)
    private String macroText;
}
