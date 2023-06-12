package ca.bc.gov.educ.penreg.api.orchestrator;

import static ca.bc.gov.educ.penreg.api.constants.EventType.ARCHIVE_PEN_REQUEST_BATCH;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GATHER_REPORT_DATA;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GENERATE_PEN_REQUEST_BATCH_REPORTS;
import static ca.bc.gov.educ.penreg.api.constants.EventType.GET_STUDENTS;
import static ca.bc.gov.educ.penreg.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT;
import static ca.bc.gov.educ.penreg.api.constants.EventType.NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationFieldCode;
import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueFieldCode;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssueTypeCode;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

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
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;
  @Autowired
  RestUtils restUtils;
  @Autowired
  ApplicationProperties props;
  @Autowired
  RestTemplate restTemplate;
  PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
  PenRequestBatchStudentMapper batchStudentMapper = PenRequestBatchStudentMapper.mapper;
  /**
   * The Saga service.
   */
  @Autowired
  private SagaService sagaService;

  @Autowired
  private PenRequestBatchService prbService;

  private final String payload_archive_return = "{\n" +
    "  \"createUser\": null,\n" +
    "  \"updateUser\": \"ADITSHAR\",\n" +
    "  \"createDate\": null,\n" +
    "  \"updateDate\": null,\n" +
    "  \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "  \"schoolName\": \"Maple Leaf International School - Dalian\",\n" +
    "  \"penRequestBatch\": {\n" +
    "    \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "    \"submissionNumber\": \"ADIT0037\",\n" +
    "    \"penRequestBatchStatusCode\": \"REARCHIVED\",\n" +
    "    \"penRequestBatchStatusReason\": null,\n" +
    "    \"penRequestBatchTypeCode\": \"SCHOOL\",\n" +
    "    \"fileName\": \"10396672.PEN\",\n" +
    "    \"fileType\": \"PEN\",\n" +
    "    \"insertDate\": \"2021-06-25T11:06:58\",\n" +
    "    \"extractDate\": \"2021-06-25T11:10:00\",\n" +
    "    \"processDate\": \"2021-06-28T11:29:57.517346\",\n" +
    "    \"sourceApplication\": \"PENWEB\",\n" +
    "    \"ministryPRBSourceCode\": \"TSWPENWEB\",\n" +
    "    \"mincode\": \"10396672\",\n" +
    "    \"schoolName\": \"Maple Leaf International School - Dalian\",\n" +
    "    \"contactName\": null,\n" +
    "    \"email\": null,\n" +
    "    \"officeNumber\": null,\n" +
    "    \"sourceStudentCount\": \"6\",\n" +
    "    \"studentCount\": \"6\",\n" +
    "    \"newPenCount\": \"0\",\n" +
    "    \"errorCount\": \"5\",\n" +
    "    \"matchedCount\": \"0\",\n" +
    "    \"repeatCount\": \"0\",\n" +
    "    \"fixableCount\": \"1\",\n" +
    "    \"sisVendorName\": \"Vendor Name\",\n" +
    "    \"sisProductName\": \"Product Name\",\n" +
    "    \"sisProductID\": \"Product ID\",\n" +
    "    \"penRequestBatchProcessTypeCode\": \"FLAT_FILE\",\n" +
    "    \"schoolGroupCode\": \"K12\",\n" +
    "    \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "    \"updateUser\": \"ADITSHAR\",\n" +
    "    \"searchedCount\": null\n" +
    "  },\n" +
    "  \"penRequestBatchStudents\": [\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc0078\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"ERROR\",\n" +
    "      \"localID\": \"0IDJQJLTEN\",\n" +
    "      \"submittedPen\": \"350800074\",\n" +
    "      \"legalFirstName\": \"GERDA\",\n" +
    "      \"legalMiddleNames\": \"RAU\",\n" +
    "      \"legalLastName\": \"PROSACCO\",\n" +
    "      \"usualFirstName\": \"BLANK\",\n" +
    "      \"usualMiddleNames\": \"SHAUN\",\n" +
    "      \"usualLastName\": \"CORWIN\",\n" +
    "      \"dob\": \"19781124\",\n" +
    "      \"genderCode\": \"F\",\n" +
    "      \"gradeCode\": \"10\",\n" +
    "      \"postalCode\": \"V2V8J2\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": null,\n" +
    "      \"questionableMatchStudentId\": null,\n" +
    "      \"infoRequest\": null,\n" +
    "      \"recordNumber\": 5,\n" +
    "      \"bestMatchPEN\": null,\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc007a\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"FIXABLE\",\n" +
    "      \"localID\": \"0HTQ4HIQ\",\n" +
    "      \"submittedPen\": \"103000469\",\n" +
    "      \"legalFirstName\": \"KATRYCE\",\n" +
    "      \"legalMiddleNames\": null,\n" +
    "      \"legalLastName\": \"BLEWETT\",\n" +
    "      \"usualFirstName\": \"KATRYCE\",\n" +
    "      \"usualMiddleNames\": null,\n" +
    "      \"usualLastName\": \"BLANK\",\n" +
    "      \"dob\": \"19800213\",\n" +
    "      \"genderCode\": \"F\",\n" +
    "      \"gradeCode\": \"12\",\n" +
    "      \"postalCode\": \"V0X1L0\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"ADITSHAR\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": \"AA\",\n" +
    "      \"questionableMatchStudentId\": null,\n" +
    "      \"infoRequest\": null,\n" +
    "      \"recordNumber\": null,\n" +
    "      \"bestMatchPEN\": null,\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc0077\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"INFOREQ\",\n" +
    "      \"localID\": \"07GGSVMIB\",\n" +
    "      \"submittedPen\": \"470400011\",\n" +
    "      \"legalFirstName\": \"MALGORZATA\",\n" +
    "      \"legalMiddleNames\": null,\n" +
    "      \"legalLastName\": \"BLANK\",\n" +
    "      \"usualFirstName\": null,\n" +
    "      \"usualMiddleNames\": null,\n" +
    "      \"usualLastName\": null,\n" +
    "      \"dob\": \"19470712\",\n" +
    "      \"genderCode\": \"F\",\n" +
    "      \"gradeCode\": null,\n" +
    "      \"postalCode\": \"V1B2R5\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"ADITSHAR\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": \"F1\",\n" +
    "      \"questionableMatchStudentId\": \"0a616610-793f-19aa-8179-3f6191100009\",\n" +
    "      \"infoRequest\": \"Need more Info\\n\\nUnable to assign PEN, birthdate too young for type of institution.\\n\\nUnable to assign PEN, birthdate too young for type of institution.\",\n" +
    "      \"recordNumber\": 1,\n" +
    "      \"bestMatchPEN\": \"470400011\",\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc007b\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"ERROR\",\n" +
    "      \"localID\": \"0ECDB8LHY\",\n" +
    "      \"submittedPen\": \"100401744\",\n" +
    "      \"legalFirstName\": \"BRADEN\",\n" +
    "      \"legalMiddleNames\": \"BLANK\",\n" +
    "      \"legalLastName\": \"TUPPER\",\n" +
    "      \"usualFirstName\": null,\n" +
    "      \"usualMiddleNames\": null,\n" +
    "      \"usualLastName\": null,\n" +
    "      \"dob\": \"19740512\",\n" +
    "      \"genderCode\": \"M\",\n" +
    "      \"gradeCode\": \"12\",\n" +
    "      \"postalCode\": \"V9P1A6\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": null,\n" +
    "      \"questionableMatchStudentId\": null,\n" +
    "      \"infoRequest\": null,\n" +
    "      \"recordNumber\": 3,\n" +
    "      \"bestMatchPEN\": null,\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc0076\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"ERROR\",\n" +
    "      \"localID\": \"0AZER1O5H5D\",\n" +
    "      \"submittedPen\": \"100600709\",\n" +
    "      \"legalFirstName\": \"BLANK\",\n" +
    "      \"legalMiddleNames\": \"JOSEPH\",\n" +
    "      \"legalLastName\": \"GOOCH\",\n" +
    "      \"usualFirstName\": \"ISAAC\",\n" +
    "      \"usualMiddleNames\": \"JOSEPH\",\n" +
    "      \"usualLastName\": \"GOOCH\",\n" +
    "      \"dob\": \"19840927\",\n" +
    "      \"genderCode\": \"M\",\n" +
    "      \"gradeCode\": \"12\",\n" +
    "      \"postalCode\": \"V4E2X1\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": null,\n" +
    "      \"questionableMatchStudentId\": null,\n" +
    "      \"infoRequest\": null,\n" +
    "      \"recordNumber\": 2,\n" +
    "      \"bestMatchPEN\": null,\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"penRequestBatchStudentID\": \"0a611b42-7a40-1860-817a-445d42dc0079\",\n" +
    "      \"penRequestBatchID\": \"0a611b42-7a40-1860-817a-445d42dc0074\",\n" +
    "      \"penRequestBatchStudentStatusCode\": \"ERROR\",\n" +
    "      \"localID\": \"0J12QEZQR\",\n" +
    "      \"submittedPen\": \"460900012\",\n" +
    "      \"legalFirstName\": \"KAIBO\",\n" +
    "      \"legalMiddleNames\": null,\n" +
    "      \"legalLastName\": \"LAMB\",\n" +
    "      \"usualFirstName\": \"KAIBO\",\n" +
    "      \"usualMiddleNames\": \"BLANK\",\n" +
    "      \"usualLastName\": \"LAMB\",\n" +
    "      \"dob\": \"19470214\",\n" +
    "      \"genderCode\": \"M\",\n" +
    "      \"gradeCode\": null,\n" +
    "      \"postalCode\": \"V7E1G2\",\n" +
    "      \"assignedPEN\": null,\n" +
    "      \"studentID\": null,\n" +
    "      \"createUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"updateUser\": \"PEN_REQUEST_BATCH_API\",\n" +
    "      \"repeatRequestSequenceNumber\": null,\n" +
    "      \"repeatRequestOriginalID\": null,\n" +
    "      \"matchAlgorithmStatusCode\": null,\n" +
    "      \"questionableMatchStudentId\": null,\n" +
    "      \"infoRequest\": null,\n" +
    "      \"recordNumber\": 6,\n" +
    "      \"bestMatchPEN\": null,\n" +
    "      \"mincode\": \"10396672\",\n" +
    "      \"submissionNumber\": \"ADIT0037\"\n" +
    "    }\n" +
    "  ],\n" +
    "  \"students\": [],\n" +
    "  \"penRequestBatchStudentValidationIssues\": {\n" +
    "    \"0a611b42-7a40-1860-817a-445d42dc0078\": \"Field value is on the list of blocked names, that are either flagged as an error or warning\",\n" +
    "    \"0a611b42-7a40-1860-817a-445d42dc0079\": \"Field value is on the list of blocked names, that are either flagged as an error or warning\",\n" +
    "    \"0a611b42-7a40-1860-817a-445d42dc007b\": \"Field value is on the list of blocked names, that are either flagged as an error or warning\",\n" +
    "    \"0a611b42-7a40-1860-817a-445d42dc0076\": \"Field value is on the list of blocked names, that are either flagged as an error or warning\"\n" +
    "  },\n" +
    "  \"penCoordinator\": {\n" +
    "    \"districtNumber\": 103,\n" +
    "    \"schoolNumber\": 96672,\n" +
    "    \"mincode\": \"10396672\",\n" +
    "    \"penCoordinatorName\": \"Aditya Sharma\",\n" +
    "    \"penCoordinatorEmail\": \"aditya.sharma@gov.bc.ca\",\n" +
    "    \"penCoordinatorFax\": \"6046756911\",\n" +
    "    \"sendPenResultsVia\": \"E\"\n" +
    "  },\n" +
    "  \"fromEmail\": \"aditya.sharma@gov.bc.ca\",\n" +
    "  \"telephone\": \"(250)356-8020\",\n" +
    "  \"facsimile\": \"(250)953-0450\",\n" +
    "  \"mailingAddress\": \"Ministry of Education and Child Care, Data Collection Unit PO Box 9170, Stn. Prov. Govt, Victoria BC, V8W 9H7\"\n" +
    "}\n";
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
  @Autowired
  private PenRequestBatchTestUtils penRequestBatchTestUtils;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    this.saga = penRequestBatchTestUtils.createSaga("19337120", "12345678", LOADED.getCode(), TEST_PEN);
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    when(this.restUtils.getProps()).thenReturn(this.props);
  }

  @Test
  public void testHandleEvent_givenBatchInSagaDataExistsAndErrorStudent_shouldArchivePenRequestBatchAndBeMarkedSTUDENTS_FOUND() throws IOException, InterruptedException, TimeoutException {
    when(this.restUtils.getPenCoordinator(anyString())).thenReturn(Optional.of(PenCoordinator.builder().penCoordinatorEmail("test@test.com").penCoordinatorName("Joe Blow").build()));
    final String errorDescription = "Invalid chars";
    when(this.restUtils.getPenRequestBatchStudentValidationIssueTypeCodeInfoByIssueTypeCode(anyString())).
      thenReturn(Optional.of(PenRequestBatchStudentValidationIssueTypeCode.builder().code(ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentValidationIssueTypeCode.INV_CHARS.getCode())
        .description(errorDescription).build()));

    final String errorFieldDescription = "Legal Given";
    when(this.restUtils.getPenRequestBatchStudentValidationIssueFieldCodeInfoByIssueFieldCode(anyString())).
            thenReturn(Optional.of(PenRequestBatchStudentValidationIssueFieldCode.builder().code(PenRequestBatchStudentValidationFieldCode.LEGAL_FIRST.getCode())
                    .description(errorFieldDescription).build()));
    this.saga = penRequestBatchTestUtils.createSaga("19337120", "12345679", PenRequestBatchStudentStatusCodes.ERROR.getCode(), TEST_PEN);
    final var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(ARCHIVE_PEN_REQUEST_BATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
    assertThat(sagaStates.size()).isEqualTo(3);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(GATHER_REPORT_DATA.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORT_DATA_GATHERED.toString());
    assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
    assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());
    final PenRequestBatchArchiveAndReturnSagaData payload = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnSagaData.class, sagaFromDB.get().getPayload());
    assertThat(payload.getFacsimile()).isNotEmpty();
    assertThat(payload.getFromEmail()).isNotEmpty();
    assertThat(payload.getTelephone()).isNotEmpty();
    assertThat(payload.getMailingAddress()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorEmail()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorName()).isNotEmpty();
    assertThat(payload.getPenRequestBatchStudents()).isNotEmpty();
    assertThat(payload.getPenRequestBatch()).isNotNull();
    assertThat(payload.getPenRequestBatchStudentValidationIssues()).containsValue(errorFieldDescription + " - " + errorDescription);
  }

  @Test
  public void testHandleEvent_givenBatchInSagaDataExistsAndUsrMtchStudent_shouldArchivePenRequestBatchAndBeMarkedSTUDENTS_FOUND() throws IOException, InterruptedException, TimeoutException {
    when(this.restUtils.getPenCoordinator(anyString())).thenReturn(Optional.of(PenCoordinator.builder().penCoordinatorEmail("test@test.com").penCoordinatorName("Joe Blow").build()));
    this.saga = penRequestBatchTestUtils.createSaga("19337120", "12345679", PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode(), TEST_PEN);
    final var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(ARCHIVE_PEN_REQUEST_BATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
    assertThat(sagaStates.size()).isEqualTo(3);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(GATHER_REPORT_DATA.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORT_DATA_GATHERED.toString());
    assertThat(sagaStates.get(2).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
    assertThat(sagaStates.get(2).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());
    final PenRequestBatchArchiveAndReturnSagaData payload = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnSagaData.class, sagaFromDB.get().getPayload());
    assertThat(payload.getFacsimile()).isNotEmpty();
    assertThat(payload.getFromEmail()).isNotEmpty();
    assertThat(payload.getTelephone()).isNotEmpty();
    assertThat(payload.getMailingAddress()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorEmail()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorName()).isNotEmpty();
    assertThat(payload.getPenRequestBatchStudents()).isNotEmpty();
    assertThat(payload.getPenRequestBatch()).isNotNull();
  }

  @Test
  public void testHandleEvent_givenBatchInSagaDataExistsAndSysNewPenStudent_shouldGatherReportDataAndBeMarkedREPORT_DATA_GATHERED() throws IOException, InterruptedException, TimeoutException {
    when(this.restUtils.getPenCoordinator(anyString())).thenReturn(Optional.of(PenCoordinator.builder().penCoordinatorEmail("test@test.com").penCoordinatorName("Joe Blow").build()));
    this.saga = penRequestBatchTestUtils.createSaga("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
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
    final PenRequestBatchArchiveAndReturnSagaData payload = JsonUtil.getJsonObjectFromString(PenRequestBatchArchiveAndReturnSagaData.class, sagaFromDB.get().getPayload());
    assertThat(payload.getFacsimile()).isNotEmpty();
    assertThat(payload.getFromEmail()).isNotEmpty();
    assertThat(payload.getTelephone()).isNotEmpty();
    assertThat(payload.getMailingAddress()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorEmail()).isNotEmpty();
    assertThat(payload.getPenCoordinator().getPenCoordinatorName()).isNotEmpty();
    assertThat(payload.getPenRequestBatchStudents()).isNotEmpty();
    assertThat(payload.getPenRequestBatch()).isNotNull();
  }


  @Test
  public void testHandleEvent_givenSTUDENTS_FOUNDEventAndCorrectSagaAndEventData_shouldBeMarkedARCHIVE_PEN_REQUEST_BATCH() throws IOException, InterruptedException, TimeoutException {
    final PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    final PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(this.batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(this.batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCoordinator(PenCoordinator.builder().penCoordinatorEmail("pen@email.com").penCoordinatorName("Joe Blow").build())
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
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(EventType.ARCHIVE_PEN_REQUEST_BATCH.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(sagaFromDB.get());
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_STUDENTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENTS_FOUND.toString());

  }

  @Test
  public void testSaveReportsWithoutPDF_givenEventAndSagaDataHasPenCoordinatorEmail_and_SfasBatchFile_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("10200030", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCoordinator(PenCoordinator.builder().penCoordinatorEmail("pen@email.com").penCoordinatorName("Joe Blow").build())
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
      .eventType(EventType.ARCHIVE_PEN_REQUEST_BATCH)
      .eventOutcome(EventOutcome.PEN_REQUEST_BATCH_UPDATED)
      .eventPayload(JsonUtil.getJsonStringFromObject(batchMapper.toStructure(penRequestBatchEntity)))
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("pen@email.com");
    assertThat(newEvent.getEventPayload()).contains("\"pendingRecords\":\"NONE\"");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(ARCHIVE_PEN_REQUEST_BATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(penRequestBatchEntity.getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(1);
  }


  @Test
  public void testGeneratePDFReport_givenEventAndSagaDataHasPenCoordinatorEmail_and_NotSfasBatchFile_shouldBeMarkedGENERATE_PEN_REQUEST_BATCH_REPORTS() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCoordinator(PenCoordinator.builder().penCoordinatorEmail("pen@email.com").penCoordinatorName("Joe Blow").build())
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
      .eventType(EventType.ARCHIVE_PEN_REQUEST_BATCH)
      .eventOutcome(EventOutcome.PEN_REQUEST_BATCH_UPDATED)
      .eventPayload(JsonUtil.getJsonStringFromObject(batchMapper.toStructure(penRequestBatchEntity)))
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PEN_REPORT_GENERATION_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("pdf");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(ARCHIVE_PEN_REQUEST_BATCH.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_BATCH_UPDATED.toString());
  }

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataHasPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penCoordinator(PenCoordinator.builder().penCoordinatorEmail("pen@email.com").penCoordinatorName("Joe Blow").build())
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
      .eventPayload(Base64.getEncoder().encodeToString("Heres a pdf report".getBytes()))
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("pen@email.com");
    assertThat(newEvent.getEventPayload()).contains("\"pendingRecords\":\"NONE\"");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
  }

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataNoPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
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
      .eventPayload(Base64.getEncoder().encodeToString("Heres a pdf report".getBytes()))
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
  }

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataEmptyPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_NO_SCHOOL_CONTACT() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    PenRequestBatchEntity penRequestBatchEntity = penRequestBatchTestUtils.createBatchEntity("19337120", "12345679", PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), TEST_PEN);
    PenRequestBatchArchiveAndReturnSagaData payload = PenRequestBatchArchiveAndReturnSagaData.builder()
      .penRequestBatch(batchMapper.toStructure(penRequestBatchEntity))
      .penRequestBatchStudents(penRequestBatchEntity.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
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
      .eventPayload(Base64.getEncoder().encodeToString("Heres a pdf report".getBytes()))
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
  }

  @Test
  public void testSendHasCoordinatorEmail_givenEventAndSagaDataHasPenCoordinatorEmail_shouldBeMarkedNOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT_2() throws InterruptedException, TimeoutException, IOException {
    final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
    this.saga.get(0).setPayload(payload_archive_return);
    this.sagaService.updateAttachedEntityDuringSagaProcess(this.saga.get(0));
    final var event = Event.builder()
      .eventType(EventType.GENERATE_PEN_REQUEST_BATCH_REPORTS)
      .eventOutcome(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED)
      .eventPayload(Base64.getEncoder().encodeToString("Heres a pdf report".getBytes()))
      .sagaId(this.saga.get(0).getSagaId())
      .build();
    this.orchestrator.handleEvent(event);
    verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThat(newEvent.getEventPayload()).contains("test@abc.com");
    assertThat(newEvent.getEventPayload()).contains("\"pendingRecords\":\"SOME\"");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.get(0).getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(NOTIFY_PEN_REQUEST_BATCH_ARCHIVE_HAS_CONTACT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga.get(0));
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GENERATE_PEN_REQUEST_BATCH_REPORTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.ARCHIVE_PEN_REQUEST_BATCH_REPORTS_GENERATED.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_REPORTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.REPORTS_SAVED.toString());
  }
}
