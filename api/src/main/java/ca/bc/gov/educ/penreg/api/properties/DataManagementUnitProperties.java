package ca.bc.gov.educ.penreg.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class DataManagementUnitProperties {

  @Value("${data.management.unit.email}")
  private String fromEmail;

  @Value("${data.management.unit.mailing.address}")
  private String mailingAddress;

  @Value("${data.management.unit.telephone}")
  private String telephone;

  @Value("${data.management.unit.facsimile}")
  private String facsimile;
}
