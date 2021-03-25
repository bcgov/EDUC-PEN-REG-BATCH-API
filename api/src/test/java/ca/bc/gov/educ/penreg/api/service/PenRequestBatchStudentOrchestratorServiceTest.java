package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.MatchAlgorithmStatusCode;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchStudentOrchestrator;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.checkerframework.checker.nullness.Opt;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventType.PROCESS_PEN_MATCH;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.INFOREQ;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

/**
 * The type Pen request batch student orchestrator service test.
 */
@SpringBootTest
@RunWith(JUnitParamsRunner.class)
@ActiveProfiles("test")
@Slf4j
@Transactional
public class PenRequestBatchStudentOrchestratorServiceTest {

  protected static final String TEST_PEN = "123456789";
  /**
   * The constant scr.
   */
  @ClassRule
  public static final SpringClassRule scr = new SpringClassRule();

  /**
   * The Smr.
   */
  @Rule
  public final SpringMethodRule smr = new SpringMethodRule();

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  private Saga saga;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private PenRequestBatchStudentService prbStudentService;

  private PenRequestBatchStudentSagaData sagaData;

  protected final String penRequestBatchID = UUID.randomUUID().toString();

  @Autowired
  private PenReqBatchStudentOrchestrator orchestrator;


  String studentID = UUID.randomUUID().toString();

  /**
   * The Pen request batch student id.
   */
  protected final String penRequestBatchStudentID = UUID.randomUUID().toString();

  /**
   * The Twin student id.
   */
  protected final String twinStudentID = UUID.randomUUID().toString();

  /**
   * The mincode.
   */
  protected final String mincode = "01292001";


  /**
   * The Orchestrator service.
   */
  @Autowired
  private PenRequestBatchStudentOrchestratorService orchestratorService;
  private UUID penRequestBatchStudentIDUUID;

  @MockBean
  private PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  private List<PenRequestBatchEntity> batchList;
  private RestUtils restUtils;

  @Before
  public void setup() throws IOException {
    final File mockPenReqBatch = new File("src/test/resources/mock_pen_req_batch.json");
    final List<PenRequestBatch> penRequestBatches = new ObjectMapper().readValue(mockPenReqBatch, new TypeReference<>() {
    });
    final File mockPenReqBatchStudent = new File("src/test/resources/mock_pen_req_batch_student.json");
    final List<PenRequestBatchStudent> penRequestBatchStudents = new ObjectMapper().readValue(mockPenReqBatchStudent, new TypeReference<>() {
    });
    final PenRequestBatchEntity entity = PenRequestBatchMapper.mapper.toModel(penRequestBatches.get(0));
    for (final var prbStudent : penRequestBatchStudents) {
      final var studentEntity = PenRequestBatchStudentMapper.mapper.toModel(prbStudent);
      studentEntity.setCreateDate(LocalDateTime.now());
      studentEntity.setCreateUser("TEST");
      studentEntity.setUpdateUser("TEST");
      studentEntity.setUpdateDate(LocalDateTime.now());
      studentEntity.setBestMatchPEN("TEST");
      studentEntity.setPenRequestBatchEntity(entity);
      entity.getPenRequestBatchStudentEntities().add(studentEntity);
    }
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser("TEST");
    entity.setUpdateUser("TEST");
    this.penRequestBatchRepository.save(entity);

    this.batchList = PenRequestBatchUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json",
            "mock_pen_req_batch_student_archived.json", 1);
    this.penRequestBatchStudentIDUUID = this.batchList.get(0).getPenRequestBatchID();

    final var payload = this.placeholderPenRequestBatchActionsSagaData();
    this.sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, payload);
    this.sagaData.setDob("19650101");
    this.sagaData.setAssignedPEN(TEST_PEN);
    this.sagaData.setLocalID("20345678");
    this.sagaData.setGradeCode("01");
    this.sagaData.setStudentID(this.studentID);
    this.sagaData.setPenRequestBatchID(entity.getPenRequestBatchID());
    this.sagaData.setPenRequestBatchStudentID(entity.getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentID());
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
            UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));

  }

  @After
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void tearDown(){
    this.penRequestBatchRepository.deleteAll();
  }
  /**
   * Test are both field value equal.
   *
   * @param field1 the field 1
   * @param field2 the field 2
   * @param res    the res
   */
  @Test
  @Parameters({
      "null, null, true",
      ",, true",
      "hi,hi, true",
      "hello,hello, true",
      "hello,Hello, false",
      "hello,hi, false",
  })
  public void testAreBothFieldValueEqual(String field1, String field2, final boolean res) {
    if ("null".equals(field1)) {
      field1 = null;
    }
    if ("null".equals(field2)) {
      field2 = null;
    }
    final var result = this.orchestratorService.areBothFieldValueEqual(field1, field2);
    assertThat(result).isEqualTo(res);

  }

  @Test
  public void testProcessPenMatchResultForF1() throws IOException, InterruptedException, TimeoutException {
    this.penRequestBatchStudentIDUUID = this.getFirstPenRequestBatchStudentID(INFOREQ.getCode());
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("F1");
    eventPayload.setMatchingRecords(new ArrayList<>());
    final var event = Event.builder()
            .eventType(PROCESS_PEN_MATCH)
            .eventOutcome(EventOutcome.PEN_MATCH_PROCESSED)
            .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
            .sagaId(this.saga.getSagaId())
            .build();
    Optional<Event> eventOptionalTest = this.orchestratorService.processPenMatchResult(saga,this.sagaData,eventPayload);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
   // assertThat(sagaFromDB).isPresent();
  }


  //need to write UT for any handleSystemMatchedStatus, handleDefault and ProcessPenMatch
  /**
   * Scrub name field.
   *
   * @param fieldValue    the field value
   * @param scrubbedValue the scrubbed value
   */
  @Test
  @Parameters({
      "a,A",
      "a  ,A",
      "hi\t,HI",
      "hello.,HELLO",
      "  he    llo      ,HE LLO",
  })
  public void scrubNameField(final String fieldValue, final String scrubbedValue) {
    final var result = this.orchestratorService.scrubNameField(fieldValue);
    assertThat(result).isEqualTo(scrubbedValue);
  }
  protected String placeholderPenRequestBatchActionsSagaData() {
    return " {\n" +
            "    \"createUser\": \"test\",\n" +
            "    \"updateUser\": \"test\",\n" +
            "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
            "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
            "    \"legalFirstName\": \"Jack\",\n" +
            "    \"mincode\": \"" + mincode + "\",\n" +
            "    \"genderCode\": \"X\",\n" +
            "    \"matchedStudentIDList\": [\"" + twinStudentID + "\"]\n" +
            "  }";
  }

  protected String dummyPenRequestBatchStudentDataJson(final String status) {
    return " {\n" +
            "    \"createUser\": \"test\",\n" +
            "    \"updateUser\": \"test\",\n" +
            "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
            "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
            "    \"legalFirstName\": \"Jack\",\n" +
            "    \"penRequestBatchStudentStatusCode\": \"" + status + "\",\n" +
            "    \"genderCode\": \"X\"\n" +
            "  }";
  }
  private UUID getFirstPenRequestBatchStudentID(final String status) {
    return this.batchList.get(0).getPenRequestBatchStudentEntities().stream()
            .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(status)).findFirst().orElseThrow().getPenRequestBatchStudentID();
  }
}
