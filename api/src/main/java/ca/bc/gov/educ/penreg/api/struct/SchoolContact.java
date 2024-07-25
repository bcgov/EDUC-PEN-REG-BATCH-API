package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.*;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchoolContact implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  private String schoolContactId;

  private String schoolId;

  @Size(max = 10)
  @NotNull(message = "schoolContactTypeCode cannot be null")
  private String schoolContactTypeCode;

  @Size(max = 10)
  @Pattern(regexp = "^$|\\d{10}", message = "Invalid phone number format")
  private String phoneNumber;

  private String jobTitle;

  @Size(max = 10)
  private String phoneExtension;

  @Size(max = 10)
  @Pattern(regexp = "^$|\\d{10}", message = "Invalid phone number format")
  private String alternatePhoneNumber;

  @Size(max = 10)
  private String alternatePhoneExtension;

  @Size(max = 255)
  @Email(message = "Email address should be a valid email address")
  private String email;

  @Size(max = 255)
  private String firstName;

  @Size(max = 255)
  @NotNull(message = "lastName cannot be null")
  private String lastName;

  private String effectiveDate;

  private String expiryDate;

  @Size(max = 100)
  public String createUser;

  @Size(max = 100)
  public String updateUser;

  @Null(message = "createDate should be null.")
  public String createDate;

  @Null(message = "updateDate should be null.")
  public String updateDate;

}
