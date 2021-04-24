package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {

  private static final String CONTENT_TYPE = "Content-Type";
  private final ObjectMapper obMapper = new ObjectMapper();
  /**
   * The Props.
   */

  private final ApplicationProperties props;


  private final WebClient webClient;

  private final MessagePublisher messagePublisher;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props the props
   */
  @Autowired
  public RestUtils(final ApplicationProperties props, final WebClient webClient, final MessagePublisher messagePublisher) {
    this.props = props;
    this.webClient = webClient;
    this.messagePublisher = messagePublisher;
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
      .uri(this.props.getStudentApiURL(), uri -> uri.path("/{studentID}").build(studentID))
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
      .uri(this.props.getStudentApiURL(), uri -> uri.path("/{studentID}").build(studentFromStudentAPI.getStudentID()))
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
  //@Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
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
      .uri(this.props.getStudentApiURL(), uri -> uri.queryParam("pen", pen).build())
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
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public String getNextPenNumberFromPenServiceAPI(final String guid) {
    return this.webClient.get()
      .uri(this.props.getPenServicesApiURL(), uri -> uri.path("/api/v1/pen-services/next-pen-number")
        .queryParam("transactionID", guid).build())
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
        .uri(this.props.getSchoolApiURL(), uri -> uri.path("/{mincode}").build(mincode))
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
      } else {
        throw ex;
      }
    }
    return school;
  }

  /**
   * this method calls student-api via NATS, a synchronous req/reply pattern.
   *
   * @param studentIDs the student ids to be fetched.
   * @return the student objects
   */
  public List<Student> getStudentsByStudentIDs(List<UUID> studentIDs) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    Event event = Event.builder().sagaId(UUID.randomUUID()).eventType(EventType.GET_STUDENTS).eventPayload(JsonUtil.getJsonStringFromObject(studentIDs)).build();
    val responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class,
      this.messagePublisher.requestMessage(STUDENT_API_TOPIC.toString(), JsonUtil.getJsonString(event).orElseThrow().getBytes(StandardCharsets.UTF_8)).get(30, TimeUnit.SECONDS).getData());
    if (responseEvent.getEventOutcome() == EventOutcome.STUDENT_NOT_FOUND) {
      return Collections.emptyList();
    }
    return obMapper.readValue(event.getEventPayload(), new TypeReference<>() {
    });
  }

  public Optional<PenCoordinator> getPenCoordinator(final String mincode) {
    try {
      final var response = this.webClient.get()
        .uri(this.props.getSchoolApiURL(), uri -> uri.path("/{mincode}/pen-coordinator").build(mincode))
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToMono(PenCoordinator.class)
        .block();
      log.info("record found for :: {}", mincode);
      return Optional.ofNullable(response);
    } catch (final WebClientResponseException ex) {
      log.info("no record found for :: {}", mincode);
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Optional.empty();
      } else {
        throw ex;
      }
    }
  }
}
