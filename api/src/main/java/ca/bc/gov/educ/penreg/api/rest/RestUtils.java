package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.exception.PenRegAPIRuntimeException;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.GradeCode;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueFieldCode;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueTypeCode;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.time.*;
import java.time.format.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
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
  public static final String GRADE_CODES = "gradeCodes";
  private static final String CONTENT_TYPE = "Content-Type";
  private final ObjectMapper obMapper = new ObjectMapper();
  /**
   * The Props.
   */

  @Getter
  private final ApplicationProperties props;


  private final WebClient webClient;


  private final MessagePublisher messagePublisher;

  private final Map<String, School> schoolMap = new ConcurrentHashMap<>();

  private final Map<String, PenRequestBatchStudentValidationIssueTypeCode> penRequestBatchStudentValidationIssueTypeCodeMap = new ConcurrentHashMap<>();

  private final Map<String, PenRequestBatchStudentValidationIssueFieldCode> penRequestBatchStudentValidationIssueFieldCodeMap = new ConcurrentHashMap<>();
  private final Map<String, List<GradeCode>> gradeCodesMap = new ConcurrentHashMap<>();
  private final ReadWriteLock gradeLock = new ReentrantReadWriteLock();
  /**
   * The School lock.
   */
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

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
    this.populatePenRequestBatchStudentValidationIssueTypeCodeMap();
    this.populatePenRequestBatchStudentValidationIssueFieldCodeMap();
    this.setGradeCodesMap();
    log.info("Called student api and loaded {} grade codes", this.gradeCodesMap.values().size());
  }

  /**
   * Sets grade codes map.
   */
  public void setGradeCodesMap() {
    val writeLock = this.gradeLock.writeLock();
    try {
      writeLock.lock();
      this.gradeCodesMap.clear();
      final List<GradeCode> gradeCodes = this.webClient.get().uri(this.props.getStudentApiURL() + "/grade-codes").header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToFlux(GradeCode.class).collectList().block();
      this.gradeCodesMap.put(GRADE_CODES, gradeCodes);
    }
    catch (Exception ex) {
      log.error("Unable to load map cache gradeCodes {}", ex);
    }
    finally {
      writeLock.unlock();
    }
  }
  public List<GradeCode> getGradeCodes() {
    if(this.gradeCodesMap.isEmpty() || this.gradeCodesMap.get(GRADE_CODES) == null || this.gradeCodesMap.get(GRADE_CODES).isEmpty()) {
      setGradeCodesMap();
    }
    return this.gradeCodesMap.get(GRADE_CODES);
  }
  /**
   * Populate school map.
   */
  public void populateSchoolMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMap.put(school.getMincode(), school);
      }
    }
    catch (Exception ex) {
      log.error("Unable to load map cache school {}", ex);
    }
    finally {
      writeLock.unlock();
    }
    log.info("loaded  {} schools to memory", this.schoolMap.values().size());
  }

  /**
   * Populate pen request batch student validation issue type code map.
   */
  public void populatePenRequestBatchStudentValidationIssueTypeCodeMap() {
    var issueTypeCodes = this.getPenRequestBatchStudentValidationIssueTypeCodes();
    penRequestBatchStudentValidationIssueTypeCodeMap.putAll(issueTypeCodes.stream().collect(Collectors.toMap(PenRequestBatchStudentValidationIssueTypeCode::getCode, Function.identity())));
    var mergedCodes = penRequestBatchStudentValidationIssueTypeCodeMap.keySet();
    var newCodes = issueTypeCodes.stream().map(PenRequestBatchStudentValidationIssueTypeCode::getCode).collect(Collectors.toSet());
    var difference = Sets.difference(mergedCodes, newCodes);
    difference.forEach(penRequestBatchStudentValidationIssueTypeCodeMap::remove);
    log.info("loaded  {} penRequestBatchStudentValidationIssueTypeCodes to memory", this.penRequestBatchStudentValidationIssueTypeCodeMap.values().size());
  }

  /**
   * Populate pen request batch student validation issue field code map.
   */
  public void populatePenRequestBatchStudentValidationIssueFieldCodeMap() {
    var issueFieldCodes = this.getPenRequestBatchStudentValidationIssueFieldCodes();
    penRequestBatchStudentValidationIssueFieldCodeMap.putAll(issueFieldCodes.stream().collect(Collectors.toMap(PenRequestBatchStudentValidationIssueFieldCode::getCode, Function.identity())));
    var mergedCodes = penRequestBatchStudentValidationIssueFieldCodeMap.keySet();
    var newCodes = issueFieldCodes.stream().map(PenRequestBatchStudentValidationIssueFieldCode::getCode).collect(Collectors.toSet());
    var difference = Sets.difference(mergedCodes, newCodes);
    difference.forEach(penRequestBatchStudentValidationIssueFieldCodeMap::remove);
    log.info("loaded  {} penRequestBatchStudentValidationIssueFieldCodes to memory", this.penRequestBatchStudentValidationIssueFieldCodeMap.values().size());
  }

  /**
   * Gets schools.
   *
   * @return the schools
   */
  public List<School> getSchools() {
    log.info("Calling Institute api to get list of schools");
    return this.webClient.get()
            .uri(this.props.getInstituteApiUrl() + "/school")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(School.class)
            .collectList()
            .block();
  }

  /**
   * Gets pen request batch student validation issue type codes.
   *
   * @return the type codes
   */
  public List<PenRequestBatchStudentValidationIssueTypeCode> getPenRequestBatchStudentValidationIssueTypeCodes() {
    log.info("calling pen service api to load penRequestBatchStudentValidationIssueTypeCodes to memory");
    return this.webClient.get()
            .uri(this.props.getPenServicesApiURL() + "/api/v1/pen-services/validation/issue-type-code")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(PenRequestBatchStudentValidationIssueTypeCode.class)
            .collectList()
            .block();
  }

  /**
   * Gets pen request batch student validation issue field codes.
   *
   * @return the field codes
   */
  public List<PenRequestBatchStudentValidationIssueFieldCode> getPenRequestBatchStudentValidationIssueFieldCodes() {
    log.info("calling pen service api to load penRequestBatchStudentValidationIssueFieldCodes to memory");
    return this.webClient.get()
            .uri(this.props.getPenServicesApiURL() + "/api/v1/pen-services/validation/issue-field-code")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(PenRequestBatchStudentValidationIssueFieldCode.class)
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
    } catch (final InterruptedException | ExecutionException | TimeoutException | IOException e) {
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
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(mincode));
  }

  /**
   * Gets penRequestBatchStudentValidationIssueTypeCode by issue type code.
   *
   * @param issueTypeCode the issue type code
   * @return the PenRequestBatchStudentValidationIssueTypeCode
   */
  public Optional<PenRequestBatchStudentValidationIssueTypeCode> getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode(final String issueTypeCode) {
    return Optional.ofNullable(this.penRequestBatchStudentValidationIssueTypeCodeMap.get(issueTypeCode));
  }

  /**
   * Gets penRequestBatchStudentValidationIssueTypeCode by issue field code.
   *
   * @param issueFieldCode the issue field code
   * @return the PenRequestBatchStudentValidationIssueFieldCode
   */
  public Optional<PenRequestBatchStudentValidationIssueFieldCode> getPenRequestBatchStudentValidationIssueFieldCodeInfoByIssueFieldCode(final String issueFieldCode) {
    return Optional.ofNullable(this.penRequestBatchStudentValidationIssueFieldCodeMap.get(issueFieldCode));
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

  public List<SchoolContact> getStudentRegistrationContactList(final String mincode) {
    try {
      var school = schoolMap.get(mincode);
      if(school == null){
        log.info("getStudentRegistrationContactList :: unable to find school for mincode {} returning empty ArrayList", mincode);
        return new ArrayList<>();
      }
      log.info("Calling Institute api to get list of school student registration contacts");
      String criterion = "[{\"condition\":null,\"searchCriteriaList\":[" +
          "{\"key\":\"schoolContactTypeCode\",\"operation\":\"eq\",\"value\":\"STUDREGIS\",\"valueType\":\"STRING\",\"condition\":\"AND\"}," +
          "{\"key\":\"schoolID\",\"operation\":\"eq\",\"value\":\"" + school.getSchoolId() + "\",\"valueType\":\"UUID\",\"condition\":\"AND\"}," +
          "{\"key\":\"email\",\"operation\":\"neq\",\"value\":\"null\",\"valueType\":\"STRING\",\"condition\":\"AND\"}" +
          "]}]";
      SchoolContactSearchWrapper schoolContactSearchWrapper = this.webClient.get()
          .uri(getSchoolContactURI(criterion))
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToFlux(SchoolContactSearchWrapper.class)
          .blockFirst();

      if (schoolContactSearchWrapper == null) {
        throw new PenRegAPIRuntimeException("API call to Institute API received null response when getting student registration contacts, contact the Ministry for more info.");
      }

      return schoolContactSearchWrapper.getContent().stream().filter(contact -> {
        final String expiryDate = contact.getExpiryDate();
        return contact.getExpiryDate() == null || (expiryDate != null && LocalDate.parse(expiryDate, DateTimeFormatter.ISO_DATE_TIME).isAfter(LocalDate.now()));
      }).toList();
    }catch(Exception e){
      log.error("API call to Institute API failure getting student registration contacts :: {}", e.getMessage());
      throw new PenRegAPIRuntimeException("API call to Institute API failure getting student registration contacts, contact the Ministry for more info.");
    }
  }

  private URI getSchoolContactURI(String criterion){
    return UriComponentsBuilder.fromHttpUrl(this.props.getInstituteApiUrl() + "/school/contact/paginated")
            .queryParam("pageNumber", "0")
            .queryParam("pageSize", "10000")
            .queryParam("searchCriteriaList", criterion).build().toUri();
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

  /**
   * This is used by RestUtilsTest to clear out schoolMap
   */
  public void clearSchoolMapForRestUtilsTest() {
    log.info("clearing schoolMap");
    this.schoolMap.clear();
  }

}
