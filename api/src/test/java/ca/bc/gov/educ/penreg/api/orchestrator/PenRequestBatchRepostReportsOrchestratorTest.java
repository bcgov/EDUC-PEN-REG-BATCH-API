package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.repository.PenCoordinatorRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.PenCoordinatorService;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnAllSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PenRequestBatchRepostReportsOrchestratorTest extends BaseOrchestratorTest {
  /**
   * The Repository.
   */
  @Autowired
  SagaRepository repository;
  /**
   * The Saga event repository.
   */
  @Autowired
  SagaEventRepository sagaEventRepository;
  /**
   * The pen coordinator repository
   */
  @Autowired
  PenCoordinatorRepository penCoordinatorRepository;
  /**
   * The Saga service.
   */
  @Autowired
  private SagaService sagaService;

  @Autowired
  private PenCoordinatorService penCoordinatorService;

  /**
   * The Message publisher.
   */
  @Autowired
  private MessagePublisher messagePublisher;

  /**
   * The issue new pen orchestrator.
   */
  @Autowired
  private PenRequestBatchRepostReportsOrchestrator orchestrator;

  /**
   * The Saga.
   */
  private List<Saga> saga;
  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  RestTemplate restTemplate;

  @Autowired
  private PenRequestBatchService prbService;

  @Autowired
  private PenRequestBatchTestUtils penRequestBatchTestUtils;

  PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
  PenRequestBatchStudentMapper batchStudentMapper = PenRequestBatchStudentMapper.mapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    this.saga = penRequestBatchTestUtils.createSaga("19337120", "12345678", LOADED.getCode(), TEST_PEN);
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.penCoordinatorRepository.saveAll(structs.stream().map(PenCoordinatorMapper.mapper::toModel).collect(Collectors.toList()));
    this.penCoordinatorService.setPenCoordinatorMap(this.penCoordinatorRepository.findAll().stream()
      .map(ca.bc.gov.educ.penreg.api.batch.mappers.PenCoordinatorMapper.mapper::toTrimmedPenCoordinator)
      .collect(Collectors.toConcurrentMap(PenCoordinator::getMincode, Function.identity())));
  }

  @Test
  public void testHandleEvent_givenSTUDENTS_FOUNDEventAndCorrectSagaAndEventData_shouldBeMarkedGENERATE_PEN_REQUEST_BATCH_REPORTS() throws IOException, InterruptedException, TimeoutException {
    final PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    final PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(this.batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(this.batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCordinatorEmail("pen@email.com")
      .mailingAddress("123 st")
      .fromEmail("test@email.com")
      .facsimile("5555555555")
      .telephone("2222222222")
      .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
      .updateUser("test user")
      .build();
    this.saga.get(0).setPayload(JsonUtil.getJsonStringFromObject(payload));
    this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));

    final var event = Event.builder()
      .eventType(GET_STUDENTS)
      .eventOutcome(EventOutcome.STUDENTS_FOUND)
      .eventPayload("[]")
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    when(this.restUtils.getStudentByPEN(TEST_PEN)).thenReturn(Optional.of(Student.builder().studentID("d332e462-917a-11eb-a8b3-0242ac130003").pen(TEST_PEN).build()));
    this.orchestrator.handleEvent(event);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(EventType.GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());

  }

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataHasPenCoordinatorEmail_and_MyEdSchool_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "M2345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCordinatorEmail("pen@email.com")
      .mailingAddress("123 st")
      .fromEmail("test@email.com")
      .facsimile("5555555555")
      .telephone("2222222222")
      .students(PenRequestBatchTestUtils.createStudents(penRequestBatchEntity))
      .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
      .build();
    this.saga.get(0).setPayload(JsonUtil.getJsonStringFromObject(payload));
    this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));
    final var event = Event.builder()
      .eventType(EventType.GENERATE_PEN_REQUEST_BATCH_REPORTS)
      .eventOutcome(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED)
      .eventPayload("Heres a pdf report")
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("pen@email.com");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(penRequestBatchEntity.getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(1);
  }

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataHasPenCoordinatorEmail_and_NotMyEdSchool_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCordinatorEmail("pen@email.com")
      .mailingAddress("123 st")
      .fromEmail("test@email.com")
      .facsimile("5555555555")
      .telephone("2222222222")
      .students(PenRequestBatchTestUtils.createStudents(penRequestBatchEntity))
      .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
      .build();
    this.saga.get(0).setPayload(JsonUtil.getJsonStringFromObject(payload));
    this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));
    final var event = Event.builder()
      .eventType(EventType.GENERATE_PEN_REQUEST_BATCH_REPORTS)
      .eventOutcome(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED)
      .eventPayload("Heres a pdf report")
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("pen@email.com");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(penRequestBatchEntity.getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(3);
  }

}