package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueTypeCode;
import ca.bc.gov.educ.penreg.api.support.NatsMessageImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.NEXT_PEN_NUMBER_RETRIEVED;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_SERVICES_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("testWebclient")
public class RestUtilsTest {

  @Autowired
  RestUtils restUtils;

  @Autowired
  ApplicationProperties applicationProperties;

  @Autowired
  WebClient webClient;

  @Autowired
  MessagePublisher messagePublisher;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock
  private WebClient.RequestBodySpec requestBodyMock;
  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriMock;
  @Mock
  private WebClient.ResponseSpec responseMock;

  @Before
  public void setUp() throws Exception {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(this.applicationProperties.getInstituteApiUrl() + "/school")).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToFlux(School.class)).thenReturn(Flux.just(createSchoolArray()));
    this.restUtils.populateSchoolMap();

    when(this.requestHeadersUriMock.uri(this.applicationProperties.getPenServicesApiURL() + "/api/v1/pen-services/validation/issue-type-code")).thenReturn(this.requestHeadersMock);
    when(this.responseMock.bodyToFlux(PenRequestBatchStudentValidationIssueTypeCode.class))
      .thenReturn(Flux.just(createValidationIssueTypeCodeArray("INVCHARS", "Invalid chars")))
      .thenReturn(Flux.just(createValidationIssueTypeCodeArray("GENDER_ERR", "Gender error")));
    this.restUtils.populatePenRequestBatchStudentValidationIssueTypeCodeMap();

    openMocks(this);
  }

  private School[] createSchoolArray() {
    School[] schools = new School[1];
    schools[0] = School.builder().mincode("10200001").schoolNumber("00001").build();
    return schools;
  }

  private PenRequestBatchStudentValidationIssueTypeCode[] createValidationIssueTypeCodeArray(String code, String description) {
    PenRequestBatchStudentValidationIssueTypeCode[] codes = new PenRequestBatchStudentValidationIssueTypeCode[1];
    codes[0] = PenRequestBatchStudentValidationIssueTypeCode.builder().code(code).description(description).build();
    return codes;
  }

  @Test
  public void testGetStudentByStudentID_givenAPICallSuccess_shouldReturnData() {
    final String studentID = UUID.randomUUID().toString();
    final Student student = new Student();
    student.setPen("123456789");
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.applicationProperties.getStudentApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(Student.class)).thenReturn(Mono.just(student));
    val result = this.restUtils.getStudentByStudentID(studentID);
    assertThat(result).isNotNull();
    assertThat(result.getPen()).isEqualTo("123456789");
  }

  @Test
  public void testUpdateStudent_givenAPICallSuccess_shouldReturnUpdatedData() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    final Student student = new Student();
    student.setPen("123456789");
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.applicationProperties.getStudentApiURL()), any(Function.class))).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(Student.class)).thenReturn(Mono.just(student));
    this.restUtils.updateStudent(student);
    verify(this.webClient, atMost(invocations + 1)).put();
  }

  @Test
  public void testGetStudentByPEN_givenAPICallSuccess_shouldReturnData() throws JsonProcessingException {
    final Student student = new Student();
    student.setPen("123456789");
    Message natsResponse = NatsMessageImpl.builder().data(new ObjectMapper().writeValueAsBytes(student)).build();

    when(this.messagePublisher.requestMessage(eq(STUDENT_API_TOPIC.toString()), any())).thenReturn(CompletableFuture.completedFuture(natsResponse));
    val result = this.restUtils.getStudentByPEN("123456789");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getPen()).isEqualTo("123456789");
  }

  @Test
  public void testGetSchoolByMincode_givenAPICallSuccess_shouldReturnData() {
    final School school = new School();
    school.setMincode("123456789");
    val result = this.restUtils.getSchoolByMincode("10200001");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getMincode()).isEqualTo("10200001");
  }

  @Test
  public void testGetPenRequestBatchStudentValidationIssueTypeCodes_givenAPICallSuccess_shouldReturnData() {
    val result = this.restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode("INVCHARS");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getDescription()).isEqualTo("Invalid chars");
  }

  @Test
  public void testPopulatePenRequestBatchStudentValidationIssueTypeCodeMap_givenNewData_shouldMergeData() {
    this.restUtils.populatePenRequestBatchStudentValidationIssueTypeCodeMap();
    var result = this.restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode("INVCHARS");
    assertThat(result).isEmpty();
    result = this.restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode("GENDER_ERR");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getDescription()).isEqualTo("Gender error");
  }

  @Test
  public void testGetNextPenNumberFromPenServiceAPI_givenAPICallSuccess_shouldReturnData() throws JsonProcessingException {
    Event event = Event.builder().eventType(EventType.GET_NEXT_PEN_NUMBER).eventOutcome(NEXT_PEN_NUMBER_RETRIEVED).eventPayload("123456789").build();
    Message natsResponse = NatsMessageImpl.builder().data(new ObjectMapper().writeValueAsBytes(event)).build();

    when(this.messagePublisher.requestMessage(eq(PEN_SERVICES_API_TOPIC.toString()), any())).thenReturn(CompletableFuture.completedFuture(natsResponse));
    final String result = this.restUtils.getNextPenNumberFromPenServiceAPI(UUID.randomUUID().toString());
    assertThat(result).isNotNull().isEqualTo("123456789");
  }

  @Test
  public void testCreateStudent_givenAPICallSuccess_shouldReturnCreatedData() {
    final String studentID = UUID.randomUUID().toString();
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    final Student student = new Student();
    student.setPen("123456789");
    student.setStudentID(studentID);
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.applicationProperties.getStudentApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(Student.class)).thenReturn(Mono.just(student));
    final Student createdStudent = this.restUtils.createStudent(student);
    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(createdStudent).isNotNull();
    assertThat(createdStudent.getStudentID()).isEqualTo(studentID);
  }

  @Test
  public void testGetSchoolByMincodeWhenEmpty_givenAPICallSuccess_shouldReturnData() {
    this.restUtils.clearSchoolMapForRestUtilsTest();
    val result = this.restUtils.getSchoolByMincode("10200001");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getMincode()).isEqualTo("10200001");
  }

  private WebClient.RequestBodySpec returnMockBodySpec() {
    return this.requestBodyMock;
  }
}

