package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRequest {
    @Size(max = 100)
    protected String createUser;
    @Size(max = 100)
    protected String updateUser;
    @Null(message = "createDate should be null.")
    protected String createDate;
    @Null(message = "updateDate should be null.")
    protected String updateDate;
}
