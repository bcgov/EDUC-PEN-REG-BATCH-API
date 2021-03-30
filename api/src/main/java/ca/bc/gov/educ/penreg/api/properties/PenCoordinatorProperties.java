package ca.bc.gov.educ.penreg.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class PenCoordinatorProperties {

  @Value("${pen.coordinator.email}")
  private String fromEmail;

  @Value("${pen.coordinator.mailing.address}")
  private String mailingAddress;

  @Value("${pen.coordinator.telephone}")
  private String telephone;

  @Value("${pen.coordinator.facsimile}")
  private String facsimile;
}
