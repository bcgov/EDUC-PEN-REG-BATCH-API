package ca.bc.gov.educ.penreg.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
@Setter
public class ApplicationProperties {

  public static final String STUDENT_API = "STUDENT-API";
  @Value("${nats.streaming.server.url}")
  @Getter
  private String natsUrl;

  @Value("${nats.streaming.server.clusterId}")
  @Getter
  private String natsClusterId;

}
