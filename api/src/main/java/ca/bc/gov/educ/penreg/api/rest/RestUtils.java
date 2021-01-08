package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {

  /**
   * The constant PARAMETERS_ATTRIBUTE.
   */
  private static final String PARAMETERS_ATTRIBUTE = "parameters";

  /**
   * The Props.
   */
  private final ApplicationProperties props;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props the props
   */
  public RestUtils(@Autowired final ApplicationProperties props) {
    this.props = props;
  }

  /**
   * Gets rest template.
   *
   * @return the rest template
   */
  public RestTemplate getRestTemplate() {
    return getRestTemplate(null);
  }

  /**
   * Gets rest template.
   *
   * @param scopes the scopes
   * @return the rest template
   */
  public RestTemplate getRestTemplate(List<String> scopes) {
    log.debug("Calling get token method");
    ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
    resourceDetails.setClientId(props.getClientID());
    resourceDetails.setClientSecret(props.getClientSecret());
    resourceDetails.setAccessTokenUri(props.getTokenURL());
    if (scopes != null) {
      resourceDetails.setScope(scopes);
    }
    return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());
  }

  /**
   * Gets student by student id.
   *
   * @param studentID the student id
   * @return the student by student id
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student getStudentByStudentID(String studentID) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(props.getStudentApiURL() + "/" + studentID, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), Student.class).getBody();
  }

  /**
   * Update student.
   *
   * @param studentFromStudentAPI the student from student api
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateStudent(Student studentFromStudentAPI) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    restTemplate.exchange(props.getStudentApiURL() + "/" + studentFromStudentAPI.getStudentID(), HttpMethod.PUT, new HttpEntity<>(studentFromStudentAPI, headers), Student.class);
  }


  /**
   * Create student student.
   *
   * @param student the student
   * @return the student
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student createStudent(Student student) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(props.getStudentApiURL(), HttpMethod.POST, new HttpEntity<>(student, headers), Student.class).getBody();
  }

  /**
   * Gets student by pen.
   *
   * @param pen the pen
   * @return the student by pen
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<Student> getStudentByPEN(String pen) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
    };
    List<Student> students = restTemplate.exchange(props.getStudentApiURL() + "/?pen=" + pen, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), responseType).getBody();
    if (students != null && !students.isEmpty()) {
      return Optional.of(students.get(0));
    }
    return Optional.empty();
  }

  /**
   * Gets next pen number from pen service api.
   *
   * @param guid the guid
   * @return the next pen number from pen service api
   */
  public String getNextPenNumberFromPenServiceAPI(String guid) {
    RestTemplate restTemplate = getRestTemplate();
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(props.getPenServicesApiURL().concat("/api/v1/pen-services/next-pen-number"))
        .queryParam("transactionID", guid);
    return restTemplate.getForEntity(builder.build().encode().toUri(), String.class).getBody();
  }

  /**
   * Gets school by min code.
   *
   * @param mincode the mincode
   * @return the school by min code
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<School> getSchoolByMinCode(String mincode) {
    Optional<School> school = Optional.empty();
    try {
      var response = getRestTemplate().getForEntity(props.getSchoolApiURL().concat("/").concat(mincode), School.class);
      if (response.hasBody()) {
        school = Optional.ofNullable(response.getBody());
      }
    } catch (final HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Optional.empty();
      }
    }

    return school;
  }
}
