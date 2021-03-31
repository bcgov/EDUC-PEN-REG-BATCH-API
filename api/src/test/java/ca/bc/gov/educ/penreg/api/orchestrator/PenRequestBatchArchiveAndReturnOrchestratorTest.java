package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.*;
import ca.bc.gov.educ.penreg.api.repository.*;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.PenCoordinatorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnAllSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
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

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenRequestBatchArchiveAndReturnOrchestratorTest extends BaseOrchestratorTest {
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
    private PenRequestBatchArchiveAndReturnOrchestrator orchestrator;

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

    PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
    PenRequestBatchStudentMapper batchStudentMapper = PenRequestBatchStudentMapper.mapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        this.createSaga("19337120", "12345678", LOADED.getCode());
        final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
        final List<ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        this.penCoordinatorRepository.saveAll(structs.stream().map(PenCoordinatorMapper.mapper::toModel).collect(Collectors.toList()));
        this.penCoordinatorService.setPenCoordinatorMap(this.penCoordinatorRepository.findAll().stream()
          .map(ca.bc.gov.educ.penreg.api.batch.mappers.PenCoordinatorMapper.mapper::toTrimmedPenCoordinator)
          .collect(Collectors.toConcurrentMap(PenCoordinator::getMincode, Function.identity())));
    }

    @After
    public void tearDown() {
        this.sagaEventRepository.deleteAll();
        this.repository.deleteAll();
        this.penRequestBatchRepository.deleteAll();
        this.penCoordinatorRepository.deleteAll();
    }



    @Test
    public void testHandleEvent_givenBatchInSagaDataExistsAndUsrMtchStudent_shouldGatherReportDataAndBeMarkedREPORT_DATA_GATHERED() throws IOException, InterruptedException, TimeoutException {
        this.createSaga("19337120", "12345679", PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode());
        final var event = Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GET_STUDENTS.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(2);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(GATHER_REPORT_DATA.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORT_DATA_GATHERED.toString());
        PenRequestBatchArchiveAndReturnSagaData payload = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnSagaData.class, sagaFromDB.get().getPayload());
        assertThat(payload.getFacsimile()).isNotEmpty();
        assertThat(payload.getFromEmail()).isNotEmpty();
        assertThat(payload.getTelephone()).isNotEmpty();
        assertThat(payload.getMailingAddress()).isNotEmpty();
        assertThat(payload.getPenCordinatorEmail()).isNotEmpty();
        assertThat(payload.getPenRequestBatchStudents()).isNotEmpty();
    }

    @Test
    public void testHandleEvent_givenBatchInSagaDataExistsNoMtchStud_shouldGatherReportDataAndBeMarkedGENERATE_PDF_REPORT() throws IOException, InterruptedException, TimeoutException {
        this.createSaga("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode());
        final var event = Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GENERATE_PDF_REPORT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(4);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(GATHER_REPORT_DATA.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORT_DATA_GATHERED.toString());
        assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
        assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());
        assertThat(sagaStates.get(3).getSagaEventState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(3).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
        PenRequestBatchArchiveAndReturnSagaData payload = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnSagaData.class, sagaFromDB.get().getPayload());
        assertThat(payload.getFacsimile()).isNotEmpty();
        assertThat(payload.getFromEmail()).isNotEmpty();
        assertThat(payload.getTelephone()).isNotEmpty();
        assertThat(payload.getMailingAddress()).isNotEmpty();
        assertThat(payload.getPenCordinatorEmail()).isNotEmpty();
        assertThat(payload.getPenRequestBatchStudents()).isNotEmpty();
        assertThat(payload.getMatchedStudents().size()).isEqualTo(0);
    }

    @Test
    public void testHandleEvent_givenSTUDENTS_FOUNDEventAndCorrectSagaAndEventData_shouldBeMarkedPEN_REQUEST_BATCH_UPDATED() throws IOException, InterruptedException, TimeoutException {
        PenRequestBatchEntity penRequestBatchEntity = createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode());
        PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
                .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
                .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
                .penCordinatorEmail("pen@email.com")
                .mailingAddress("123 st")
                .fromEmail("test@email.com")
                .facsimile("5555555555")
                .telephone("2222222222")
                .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
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
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(EventType.GENERATE_PDF_REPORT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(2);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());

    }

    @Test
    public void testSendHasCoordinatorEmail_givenEventAndSagaDataHasPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        PenRequestBatchEntity penRequestBatchEntity = createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode());
        PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
                .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
                .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
                .penCordinatorEmail("pen@email.com")
                .mailingAddress("123 st")
                .fromEmail("test@email.com")
                .facsimile("5555555555")
                .telephone("2222222222")
                .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
                .build();
        this.saga.get(0).setPayload(JsonUtil.getJsonStringFromObject(payload));
        this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));
        final var event = Event.builder()
                .eventType(EventType.GENERATE_PDF_REPORT)
                .eventOutcome(EventOutcome.PDF_REPORT_GENERATED)
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
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PDF_REPORT.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PDF_REPORT_GENERATED.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
    }

    @Test
    public void testSendHasCoordinatorEmail_givenEventAndSagaDataNoPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT() throws InterruptedException, TimeoutException, IOException {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        PenRequestBatchEntity penRequestBatchEntity = createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode());
        PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
                .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
                .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
                .mailingAddress("123 st")
                .fromEmail("test@email.com")
                .facsimile("5555555555")
                .telephone("2222222222")
                .penRequestBatchID(penRequestBatchEntity.getPenRequestBatchID())
                .build();
        this.saga.get(0).setPayload(JsonUtil.getJsonStringFromObject(payload));
        this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));
        final var event = Event.builder()
                .eventType(EventType.GENERATE_PDF_REPORT)
                .eventOutcome(EventOutcome.PDF_REPORT_GENERATED)
                .eventPayload("Heres a pdf report")
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
        assertThat(newEvent.getEventPayload()).isNotEmpty();
        assertThat(newEvent.getEventPayload()).contains("test@abc.com");//from application.properties
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
        assertThat(sagaStates.size()).isEqualTo(2);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PDF_REPORT.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PDF_REPORT_GENERATED.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
    }

    private PenRequestBatchEntity createBatchEntity (String mincode, String submissionNumber, String penRequestBatchStudentStatusCode) {
        final PenRequestBatchStudentEntity penRequestBatchStudentEntity = new PenRequestBatchStudentEntity();
        penRequestBatchStudentEntity.setPenRequestBatchStudentStatusCode(penRequestBatchStudentStatusCode);
        penRequestBatchStudentEntity.setCreateDate(LocalDateTime.now());
        penRequestBatchStudentEntity.setUpdateDate(LocalDateTime.now());
        penRequestBatchStudentEntity.setCreateUser("TEST");
        penRequestBatchStudentEntity.setUpdateUser("TEST");
        penRequestBatchStudentEntity.setAssignedPEN(TEST_PEN);
        penRequestBatchStudentEntity.setDob("19650101");
        penRequestBatchStudentEntity.setGenderCode("M");
        penRequestBatchStudentEntity.setLocalID("20345678");
        penRequestBatchStudentEntity.setGradeCode("01");
        final PenRequestBatchEntity entity = new PenRequestBatchEntity();
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser("TEST");
        entity.setUpdateUser("TEST");
        entity.setPenRequestBatchStatusCode(LOADED.getCode());
        entity.setSubmissionNumber(submissionNumber);
        entity.setPenRequestBatchTypeCode(PenRequestBatchTypeCode.SCHOOL.getCode());
        entity.setSchoolGroupCode("K12");
        entity.setFileName("test");
        entity.setFileType("PEN");
        entity.setMincode(mincode);
        entity.setMinistryPRBSourceCode("PEN_WEB");
        entity.setInsertDate(LocalDateTime.now());
        entity.setExtractDate(LocalDateTime.now());
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        entity.setSourceStudentCount(1L);
        entity.setStudentCount(1L);
        entity.setSourceApplication("PEN");
        entity.setPenRequestBatchProcessTypeCode(PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode());
        penRequestBatchStudentEntity.setPenRequestBatchEntity(entity);
        entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
        this.penRequestBatchRepository.save(entity);
        return entity;
    }
    private void createSaga(String mincode, String submissionNumber, String penRequestBatchStudentStatusCode) throws JsonProcessingException {
        PenRequestBatchEntity entity = createBatchEntity(mincode, submissionNumber, penRequestBatchStudentStatusCode);
        List<PenRequestBatchArchiveAndReturnSagaData> penRequestBatchIDList = Collections.singletonList(PenRequestBatchArchiveAndReturnSagaData.builder()
                .penRequestBatchID(entity.getPenRequestBatchID()).schoolName("Cataline").build());

        final var payload = " {\n" +
                "    \"createUser\": \"test\",\n" +
                "    \"updateUser\": \"test\"\n" +
                "  }";

        PenRequestBatchArchiveAndReturnAllSagaData sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnAllSagaData.class, payload);
        sagaData.setPenRequestBatchArchiveAndReturnSagaData(penRequestBatchIDList);
        this.saga = this.sagaService.createMultipleBatchSagaRecordsInDB(PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(sagaData),
                penRequestBatchIDList);
    }
}
