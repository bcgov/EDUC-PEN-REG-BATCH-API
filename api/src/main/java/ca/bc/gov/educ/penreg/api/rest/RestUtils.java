package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {


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
  @Autowired
  public RestUtils(final ApplicationProperties props, final WebClient webClient) {
    this.props = props;
    this.webClient = webClient;
  }

  /**
   * Gets student by student id.
   *
   * @param studentID the student id
   * @return the student by student id
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student getStudentByStudentID(final String studentID) {
    return this.webClient.get()
        .uri(this.props.getStudentApiURL() + "/" + studentID)
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToMono(Student.class)
        .block();
  }

  /**
   * Update student.
   *
   * @param studentFromStudentAPI the student from student api
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateStudent(final Student studentFromStudentAPI) {
    this.webClient.put()
        .uri(this.props.getStudentApiURL() + "/" + studentFromStudentAPI.getStudentID())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(studentFromStudentAPI), Student.class)
        .retrieve()
        .bodyToMono(Student.class)
        .block();
  }


  /**
   * Create student student.
   *
   * @param student the student
   * @return the student
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student createStudent(final Student student) {
    return this.webClient.post()
        .uri(this.props.getStudentApiURL())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(student), Student.class)
        .retrieve()
        .bodyToMono(Student.class)
        .block();
  }

  /**
   * Gets student by pen.
   *
   * @param pen the pen
   * @return the student by pen
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<Student> getStudentByPEN(final String pen) {
    final var studentResponse = this.webClient.get()
        .uri(this.props.getStudentApiURL() + "?pen=" + pen)
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToFlux(Student.class)
        .collectList()
        .block();
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
  public String getNextPenNumberFromPenServiceAPI(final String guid) {
    return this.webClient.get()
        .uri(this.props.getPenServicesApiURL().concat("/api/v1/pen-services/next-pen-number?transactionID=" + guid))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToMono(String.class)
        .block();
  }

  /**
   * Gets school by min code.
   *
   * @param mincode the mincode
   * @return the school by min code
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<School> getSchoolByMincode(final String mincode) {
    Optional<School> school = Optional.empty();
    try {
      final var response = this.webClient.get()
          .uri(this.props.getSchoolApiURL().concat("/").concat(mincode))
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(School.class)
          .block();
      if (response != null) {
        school = Optional.of(response);
      }
    } catch (final WebClientResponseException ex) {
      log.info("no record found for :: {}", mincode);
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Optional.empty();
      }
    }
    return school;
  }
}
