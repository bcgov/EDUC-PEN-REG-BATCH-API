package ca.bc.gov.educ.penreg.api.rest;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
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
  public Student getStudentByStudentID(String studentID) {
    return webClient.get()
            .uri(uriBuilder -> uriBuilder.path(this.props.getStudentApiURL()+"/"+studentID)
                    .build())
            .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
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
  public void updateStudent(Student studentFromStudentAPI) {
    webClient.put()
           .uri(uriBuilder -> uriBuilder.path(this.props.getStudentApiURL()+"/"+ studentFromStudentAPI.getStudentID())
                   .build())
           .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
           .body(Mono.just(studentFromStudentAPI),Student.class)
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
  public Student createStudent(Student student) {
      return webClient.post()
              .uri(uriBuilder -> uriBuilder.path(props.getStudentApiURL())
                      .build())
              .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
              .body(Mono.just(student),Student.class)
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
  public Optional<Student> getStudentByPEN(String pen) {
    final var studentResponse=this.webClient.get()
            .uri(uriBuilder -> uriBuilder.path(props.getStudentApiURL() + "/?pen=")
                    .queryParam("pen",pen)
                    .build())
            .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
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
  public String getNextPenNumberFromPenServiceAPI(String guid) {
    return this.webClient.get()
            .uri(uriBuilder -> uriBuilder.path(props.getPenServicesApiURL().concat("/api/v1/pen-services/next-pen-number"))
                    .queryParam("transactionID",guid)
                    .build())
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
  public Optional<School> getSchoolByMincode(String mincode) {
    Optional<School> school = Optional.empty();
    try {
      final var response = this.webClient.get()
              .uri(uriBuilder -> uriBuilder.path(props.getSchoolApiURL().concat("/").concat(mincode))
                      .build())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(School.class)
              .block();
       if (response != null) {
        school= Optional.of(response);
      }
    } catch (final HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Optional.empty();
      }
    }
    return school;
  }
}
