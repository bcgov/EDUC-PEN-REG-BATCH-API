package ca.bc.gov.educ.penreg.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class NotificationProperties {

  @Value("${notification.email.school.error.unformatted.file.from.email}")
  private String fromEmail;
}
