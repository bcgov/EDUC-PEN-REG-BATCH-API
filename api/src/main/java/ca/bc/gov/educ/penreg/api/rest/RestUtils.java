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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Objects;
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

  private static final String CONTENT_TYPE="Content-Type";

  /**
   * The Props.
   */

  private final ApplicationProperties props;


  private final WebClient webClient;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props the props
   */
  public RestUtils(@Autowired final ApplicationProperties props, final WebClient webClient) {
    this.props = props;
    this.webClient = webClient;
  }

  /**
   * Gets rest template.
   *
   * @return the rest template
   */
  /**
   * Gets student by student id.
   *
   * @param studentID the student id
   * @return the student by student id
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student getStudentByStudentID(String studentID) {
    Student student=this.webClient.get().uri(this.props.getStudentApiURL()+"/"+studentID).header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToMono(Student.class).block();
    return student;
  }

  /**
   * Update student.
   *
   * @param studentFromStudentAPI the student from student api
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateStudent(Student studentFromStudentAPI) {
   Student student=this.webClient.put().uri(this.props.getStudentApiURL()+"/"+ studentFromStudentAPI.getStudentID()).header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToMono(Student.class).block();
  }


  /**
   * Create student student.
   *
   * @param student the student
   * @return the student
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student createStudent(Student student) {
    Student studentResponse=this.webClient.post().uri(this.props.getStudentApiURL()).header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToMono(Student.class).block();
    return studentResponse;
  }

  /**
   * Gets student by pen.
   *
   * @param pen the pen
   * @return the student by pen
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<Student> getStudentByPEN(String pen) {
    List<Student> studentResponse=this.webClient.get().uri(this.props.getStudentApiURL()+ "/?pen=" + pen).header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToFlux(Student.class).collectList().block();
    if (studentResponse != null && !studentResponse.isEmpty()) {
      return Optional.of(studentResponse.get(0));
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
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(props.getPenServicesApiURL().concat("/api/v1/pen-services/next-pen-number"))
        .queryParam("transactionID", guid);
    final var url=builder.toUriString();
    final var studentResponse = this.webClient.get().uri(url).header(MediaType.APPLICATION_JSON_VALUE,CONTENT_TYPE).retrieve().bodyToMono(String.class).block();
    return studentResponse;
  }

  /**
   * Gets school by min code.
   *
   * @param mincode the mincode
   * @return the school by min code
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<School> getSchoolByMincode(String mincode) {
    Optional<School> school = Optional.empty();
    try {
      final ParameterizedTypeReference<RestPageImpl<School>> responseType = new ParameterizedTypeReference<RestPageImpl<School>>() {
      };
      final var response = this.webClient.get().uri(props.getSchoolApiURL().concat("/").concat(mincode)).header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToMono(responseType).block();
      final var optionalSchool = Objects.requireNonNull(response).getContent().stream().findAny();
      if (response != null && !response.isEmpty()) {
        school= Optional.of(optionalSchool.get());
      }
    } catch (final HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Optional.empty();
      }
    }
    return school;
  }
}
