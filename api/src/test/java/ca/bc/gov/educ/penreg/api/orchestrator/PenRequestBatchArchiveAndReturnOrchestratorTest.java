package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.*;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.*;
import ca.bc.gov.educ.penreg.api.repository.*;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.PenCoordinatorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnAllSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchivedEmailEvent;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentStatusCode;
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
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
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
     * The Saga data.
     */
    private PenRequestBatchArchiveAndReturnAllSagaData sagaData;
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
    public void testHandleEvent_givenEventAndSagaData_shouldBeMarkedPEN_COORDINATOR_NOT_FOUND() throws IOException, InterruptedException, TimeoutException {
        this.createSaga("10200000", "12345679", LOADED.getCode());
        final var event = Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(4);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
        assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GENERATE_IDS_REPORT.toString());
        assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.IDS_REPORT_GENERATED.toString());
        assertThat(sagaStates.get(3).getSagaEventState()).isEqualTo(GET_PEN_COORDINATOR.toString());
        assertThat(sagaStates.get(3).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_COORDINATOR_NOT_FOUND.toString());
    }

    @Test
    public void testHandleEvent_givenEventAndSagaDataAndPenCoordinatorExists_shouldBeMarkedPEN_COORDINATOR_FOUND() throws IOException, InterruptedException, TimeoutException {
        final var event = Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(4);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
        assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GENERATE_IDS_REPORT.toString());
        assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.IDS_REPORT_GENERATED.toString());
        assertThat(sagaStates.get(3).getSagaEventState()).isEqualTo(GET_PEN_COORDINATOR.toString());
        assertThat(sagaStates.get(3).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_COORDINATOR_FOUND.toString());

    }

    @Test
    public void testHandleEvent_givenEventAndSagaDataAndPenCoordinatorExists_shouldBeMarkedPEN_COORDINATOR_FOUND_withReArchivedBatchStatus() throws IOException, InterruptedException, TimeoutException {
        final var event = Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        Optional<PenRequestBatchEntity> prbBatchOption = this.penRequestBatchRepository.findBySubmissionNumber("12345678");
        if (prbBatchOption.isPresent()) {
            prbBatchOption.get().setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode());
            this.penRequestBatchRepository.saveAndFlush(prbBatchOption.get());
        }

        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(4);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
        assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
        assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GENERATE_IDS_REPORT.toString());
        assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.IDS_REPORT_GENERATED.toString());
        assertThat(sagaStates.get(3).getSagaEventState()).isEqualTo(GET_PEN_COORDINATOR.toString());
        assertThat(sagaStates.get(3).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_COORDINATOR_FOUND.toString());

        prbBatchOption = this.penRequestBatchRepository.findBySubmissionNumber("12345678");
        assertThat(prbBatchOption.isPresent()).isTrue();
        assertThat(prbBatchOption.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.REARCHIVED.getCode());
    }

    @Test
    public void testArchivePenRequestBatch_givenEventAndSagaData_shouldBeMarkedCOMPLETED() throws IOException, InterruptedException, TimeoutException {
        final var event = Event.builder()
                .eventType(UPDATE_PEN_REQUEST_BATCH)
                .eventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND)
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
        assertThat(sagaStates.size()).isEqualTo(1);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_PEN_REQUEST_BATCH.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND.toString());
    }

    @Test
    public void testSendHasCoordinatorEmail_givenEventAndSagaData_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
                .fromEmail("from@email.com")
                .mincode("to@email.com")
                .submissionNumber("12345678")
                .schoolName("Test School")
                .build();
        final var event = Event.builder()
                .eventType(EventType.GET_PEN_COORDINATOR)
                .eventOutcome(EventOutcome.PEN_COORDINATOR_FOUND)
                .eventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent))
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
        assertThat(newEvent.getEventPayload()).isNotEmpty();
        assertThat(newEvent.getEventPayload()).contains("to@email.com");
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
        assertThat(sagaStates.size()).isEqualTo(1);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_PEN_COORDINATOR.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_COORDINATOR_FOUND.toString());
    }

    @Test
    public void testSendHasCoordinatorEmail_givenEventAndSagaData_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_CONTACT() throws InterruptedException, TimeoutException, IOException {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        PenRequestBatchArchivedEmailEvent penRequestBatchArchivedEmailEvent = PenRequestBatchArchivedEmailEvent.builder()
                .fromEmail("from@email.com")
                .mincode("to@email.com")
                .submissionNumber("12345678")
                .schoolName("Test School")
                .build();
        final var event = Event.builder()
                .eventType(EventType.GET_PEN_COORDINATOR)
                .eventOutcome(EventOutcome.PEN_COORDINATOR_NOT_FOUND)
                .eventPayload(JsonUtil.getJsonStringFromObject(penRequestBatchArchivedEmailEvent))
                .sagaId(this.saga.get(0).getSagaId())
                .build();
        this.orchestrator.handleEvent(event);
        verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT);
        assertThat(newEvent.getEventPayload()).isNotEmpty();
        assertThat(newEvent.getEventPayload()).contains("to@email.com");
        final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT.toString());
        final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
        assertThat(sagaStates.size()).isEqualTo(1);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_PEN_COORDINATOR.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_COORDINATOR_NOT_FOUND.toString());
    }

    private void createSaga(String mincode, String submissionNumber, String penRequestBatchStudentStatusCode) throws JsonProcessingException {
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
        entity.setSourceStudentCount(1L);
        entity.setStudentCount(1L);
        entity.setSourceApplication("PEN");
        entity.setPenRequestBatchProcessTypeCode(PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode());
        penRequestBatchStudentEntity.setPenRequestBatchEntity(entity);
        entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
        this.penRequestBatchRepository.save(entity);
        List<UUID> penRequestBatchIDList = Collections.singletonList(entity.getPenRequestBatchID());

        final var payload = " {\n" +
                "    \"createUser\": \"test\",\n" +
                "    \"updateUser\": \"test\"\n" +
                "  }";
        this.sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnAllSagaData.class, payload);
        this.sagaData.setPenRequestBatchIDs(penRequestBatchIDList);
        this.saga = this.sagaService.createMultipleBatchSagaRecordsInDB(PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
                penRequestBatchIDList);
    }
}
