package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.Data;

import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

@Data
public abstract class BaseRequest {
    @Size(max = 32)
    protected String createUser;
    @Size(max = 32)
    protected String updateUser;
    @Null(message = "createDate should be null.")
    protected String createDate;
    @Null(message = "updateDate should be null.")
    protected String updateDate;
}
