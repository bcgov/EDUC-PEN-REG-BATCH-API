package ca.bc.gov.educ.penreg.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
@Setter
public class ApplicationProperties {

  public static final String API_NAME = "PEN_REG_BATCH_API";
  /**
   * The Client id.
   */
  @Value("${client.id}")
  private String clientID;
  /**
   * The Client secret.
   */
  @Value("${client.secret}")
  private String clientSecret;
  /**
   * The Token url.
   */
  @Value("${url.token}")
  private String tokenURL;
  /**
   * The Student api url.
   */
  @Value("${url.api.student}")
  private String studentApiURL;

  /**
   * Amount of time that can elapse before a duplicate request is not considered a repeat
   */
  @Value("${repeat.time.window.psi}")
  private int repeatTimeWindowPSI;

  /**
   * Amount of time that can elapse before a duplicate request is not considered a repeat
   */
  @Value("${repeat.time.window.k12}")
  private int repeatTimeWindowK12;

  @Value("${url.api.pen.services}")
  private String penServicesApiURL;

  @Value("${url.api.school}")
  private String schoolApiURL;

  @Value("${nats.server}")
  private String server;

  @Value("${nats.maxReconnect}")
  private int maxReconnect;

  @Value("${nats.connectionName}")
  private String connectionName;

  @Value("${number.records.for.batch.hold}")
  private int numRecordsForBatchHold;

  @Value("${skip.validation.for.district.codes}")
  private Set<String> districtCodesToNotValidate;

}
