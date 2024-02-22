package ca.bc.gov.educ.penreg.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * The type School.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class School implements Serializable {

  private static final long serialVersionUID = 1L;

  @Null(message = "schoolId should be null")
  private String schoolId;
  @NotNull(message = "districtId can not be null.")
  private String districtId;

  @Size(max = 10)
  @NotNull(message = "schoolReportingRequirementCode cannot be null")
  private String schoolReportingRequirementCode;

  private String mincode;

  private String independentAuthorityId;

  @Size(max = 5)
  @NotNull(message = "schoolNumber can not be null.")
  private String schoolNumber;

  @Size(max = 10)
  private String faxNumber;

  @Size(max = 10)
  private String phoneNumber;

  @Size(max = 255)
  @Email(message = "Email address should be a valid email address")
  private String email;

  @Size(max = 255)
  private String website;

  @Size(max = 255)
  @NotNull(message = "displayName cannot be null")
  private String displayName;

  @Size(max = 255)
  private String displayNameNoSpecialChars;

  @Size(max = 10)
  @NotNull(message = "schoolOrganizationCode cannot be null")
  private String schoolOrganizationCode;

  @Size(max = 10)
  @NotNull(message = "schoolCategoryCode cannot be null")
  private String schoolCategoryCode;

  @Size(max = 10)
  @NotNull(message = "facilityTypeCode cannot be null")
  private String facilityTypeCode;

  private String openedDate;

  private String closedDate;

  @Size(max = 32)
  public String createUser;

  @Size(max = 32)
  public String updateUser;

  @Null(message = "createDate should be null.")
  public String createDate;

  @Null(message = "updateDate should be null.")
  public String updateDate;
}
