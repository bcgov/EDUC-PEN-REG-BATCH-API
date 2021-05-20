package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.GradeCode;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;

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

  /**
   * The Grade lock.
   */
  private final ReadWriteLock gradeLock = new ReentrantReadWriteLock();

  private final MessagePublisher messagePublisher;

  private final Map<String, School> schoolMap = new ConcurrentHashMap<>();
  /**
   * The School lock.
   */
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;
  @Getter
  private List<String> gradeCodes = new CopyOnWriteArrayList<>();

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
   * Init.
   */
  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  private void initialize() {
    this.populateSchoolMap();
    this.setGradeCodesMap();
  }

  /**
   * Sets grade codes map.
   */
  public void setGradeCodesMap() {
    val writeLock = this.gradeLock.writeLock();
    try {
      writeLock.lock();
      val result = this.webClient.get().uri(this.props.getStudentApiURL(), uri -> uri.path("/grade-codes").build()).header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToFlux(GradeCode.class).collectList().block();
      if (!CollectionUtils.isEmpty(result)) {
        this.gradeCodes = result.stream().map(GradeCode::getGradeCode).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
      }
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Populate school map.
   */
  public void populateSchoolMap() {
    for (val school : this.getSchools()) {
      this.schoolMap.putIfAbsent(school.getDistNo() + school.getSchlNo(), school);
    }
    log.info("loaded  {} schools to memory", this.schoolMap.values().size());
  }

  /**
   * Gets schools.
   *
   * @return the schools
   */
  public List<School> getSchools() {
    log.info("calling school api to load schools to memory");
    return this.webClient.get()
      .uri(this.props.getSchoolApiURL())
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve()
      .bodyToFlux(School.class)
      .collectList()
      .block();
  }

  /**
   * Scheduled.
   */
  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      this.init();
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Gets student by student id.
   *
   * @param studentID the student id
   * @return the student by student id
   */
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
  public Optional<Student> getStudentByPEN(final String pen) {
    val getStudentByPenEvent = Event.builder().eventType(EventType.GET_STUDENT).eventPayload(pen).sagaId(UUID.randomUUID()).build();
    try {
      log.info("calling getStudentByPEN :: {} via NATS for pen :: {}", STUDENT_API_TOPIC, pen);
      val eventResponse = this.messagePublisher.requestMessage(STUDENT_API_TOPIC.toString(), JsonUtil.getJsonString(getStudentByPenEvent).orElseThrow().getBytes(StandardCharsets.UTF_8)).get(30, TimeUnit.SECONDS).getData();
      log.info("got response from NATS for pen :: {}, student found :: {}", pen, eventResponse.length > 0);
      if (eventResponse.length > 0) {
        return Optional.of(JsonUtil.getJsonObjectFromByteArray(Student.class, eventResponse));
      }
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      Thread.currentThread().interrupt();
      log.error("Exception while get student by pen", e);
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
    val event = Event.builder().eventType(EventType.GET_NEXT_PEN_NUMBER).eventPayload(guid).build();
    val responseEvent = this.requestEventResponseFromServicesAPI(event);
    return responseEvent.map(Event::getEventPayload).orElse(null);
  }

  /**
   * Gets school by min code.
   *
   * @param mincode the mincode
   * @return the school by min code
   */
  public Optional<School> getSchoolByMincode(final String mincode) {
    return Optional.ofNullable(this.schoolMap.get(mincode));
  }

  /**
   * this method calls student-api via NATS, a synchronous req/reply pattern.
   *
   * @param studentIDs the student ids to be fetched.
   * @return the student objects
   */
  public List<Student> getStudentsByStudentIDs(final List<UUID> studentIDs) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    final var event = Event.builder().sagaId(UUID.randomUUID()).eventType(EventType.GET_STUDENTS).eventPayload(JsonUtil.getJsonStringFromObject(studentIDs)).build();
    val responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class,
      this.messagePublisher.requestMessage(STUDENT_API_TOPIC.toString(), JsonUtil.getJsonString(event).orElseThrow().getBytes(StandardCharsets.UTF_8)).get(30, TimeUnit.SECONDS).getData());
    if (responseEvent.getEventOutcome() == EventOutcome.STUDENT_NOT_FOUND) {
      return Collections.emptyList();
    }
    return this.obMapper.readValue(responseEvent.getEventPayload(), new TypeReference<>() {
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

  public Optional<Event> requestEventResponseFromServicesAPI(final Event event) {
    return this.requestEventResponseFromAPI(event, PEN_SERVICES_API_TOPIC, "Exception while calling services api via nats");
  }

  public Optional<Event> requestEventResponseFromMatchAPI(final Event event) {
    return this.requestEventResponseFromAPI(event, PEN_MATCH_API_TOPIC, "Exception while calling match api via nats");
  }

  public Optional<Event> requestEventResponseFromStudentAPI(final Event event) {
    return this.requestEventResponseFromAPI(event, STUDENT_API_TOPIC, "Exception while calling student api via nats");
  }

  /**
   * This is a synchronous req/reply pattern call via NATS
   */
  private Optional<Event> requestEventResponseFromAPI(final Event event, final SagaTopicsEnum topic, final String exceptionMessage) {
    try {
      log.info("calling :: {} via NATS", topic);
      val response = JsonUtil.getJsonObjectFromByteArray(Event.class,
        this.messagePublisher.requestMessage(topic.toString(), JsonUtil.getJsonString(event).orElseThrow().getBytes(StandardCharsets.UTF_8)).get(30, TimeUnit.SECONDS).getData());
      log.info("got response from NATS :: {}", response.getEventOutcome());
      return Optional.of(response);
    } catch (final Exception e) {
      Thread.currentThread().interrupt();
      log.error(exceptionMessage, e);
    }
    return Optional.empty();
  }


}
