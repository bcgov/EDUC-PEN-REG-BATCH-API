package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.Student;
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
import java.util.function.Function;

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
    openMocks(this);
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
  public void testGetStudentByPEN_givenAPICallSuccess_shouldReturnData() {
    final Student student = new Student();
    student.setPen("123456789");
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.applicationProperties.getStudentApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToFlux(Student.class)).thenReturn(Flux.just(student));
    val result = this.restUtils.getStudentByPEN("123456789");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getPen()).isEqualTo("123456789");
  }

  @Test
  public void testGetSchoolByMincode_givenAPICallSuccess_shouldReturnData() {
    final School school = new School();
    school.setMincode("123456789");
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.applicationProperties.getSchoolApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(School.class)).thenReturn(Mono.just(school));
    val result = this.restUtils.getSchoolByMincode("123456789");
    assertThat(result).isNotNull().isPresent();
    assertThat(result.get().getMincode()).isEqualTo("123456789");
  }

  @Test
  public void testGetNextPenNumberFromPenServiceAPI_givenAPICallSuccess_shouldReturnData() {
    final School school = new School();
    school.setMincode("123456789");
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.applicationProperties.getPenServicesApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("123456789"));
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

  private WebClient.RequestBodySpec returnMockBodySpec() {
    return this.requestBodyMock;
  }
}

