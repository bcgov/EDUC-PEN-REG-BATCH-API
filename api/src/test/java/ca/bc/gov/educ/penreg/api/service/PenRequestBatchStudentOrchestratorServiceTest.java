package ca.bc.gov.educ.penreg.api.service;


import ca.bc.gov.educ.penreg.api.constants.BadLocalID;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.orchestrator.BaseOrchestratorTest;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.PenMatchRecord;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * The type Pen request batch student orchestrator service test.
 */
@RunWith(JUnitParamsRunner.class)
@Slf4j
@Transactional
public class PenRequestBatchStudentOrchestratorServiceTest extends BaseOrchestratorTest {
  @ClassRule
  public static final SpringClassRule scr = new SpringClassRule();
  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<Student> createStudentCaptor;
  /**
   * The Smr.
   */
  @Rule
  public final SpringMethodRule smr = new SpringMethodRule();
  private Saga saga;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  private PenRequestBatchTestUtils penRequestBatchUtils;

  private PenRequestBatchStudentSagaData sagaData;

  protected final String penRequestBatchID = UUID.randomUUID().toString();


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



  @Before
  public void setup() throws IOException {
    this.penRequestBatchUtils.createBatchStudentsInSingleTransaction(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json", "mock_pen_req_batch_student_archived.json", 1, null);

    final List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    final var payload = this.placeholderPenRequestBatchActionsSagaData();
    this.sagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, payload);
    this.sagaData.setDob("19650101");
    this.sagaData.setAssignedPEN(TEST_PEN);
    this.sagaData.setLocalID("20345678");
    this.sagaData.setGradeCode("01");
    this.sagaData.setStudentID(this.studentID);
    this.sagaData.setPenRequestBatchID(batches.get(0).getPenRequestBatchID());
    this.sagaData.setPenRequestBatchStudentID(batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentID());
    this.saga = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), "Test", JsonUtil.getJsonStringFromObject(this.sagaData),
            UUID.fromString(this.penRequestBatchStudentID), UUID.fromString(this.penRequestBatchID));

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
  public void testProcessPenMatchResultForF1() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("F1");
    eventPayload.setMatchingRecords(new ArrayList<>());
    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    final List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    assertThat(batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentStatusCode()).isEqualTo(PenRequestBatchStudentStatusCodes.FIXABLE.getCode());
  }

  @Test
  public void testProcessPenMatchResultForD1() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("D1");
    final PenMatchRecord record = new PenMatchRecord();
    List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    final PenRequestBatchStudentEntity studEntity = batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow();
    record.setStudentID(studEntity.getPenRequestBatchStudentID().toString());
    when(this.restUtils.getStudentByStudentID(studEntity.getPenRequestBatchStudentID().toString())).thenReturn(Student.builder().studentID(studEntity.getPenRequestBatchStudentID().toString()).pen(TEST_PEN).build());
    record.setMatchingPEN("123456789");
    eventPayload.setMatchingRecords(new ArrayList<>());
    eventPayload.getMatchingRecords().add(record);
    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    batches = this.penRequestBatchRepository.findAll();
    assertThat(batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentStatusCode()).isEqualTo(PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode());
  }

  @Test
  public void testProcessPenMatchResult_givenSystemMatchScenario_studentShouldBeUpdatedWithDifferentValues() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("D1");
    final PenMatchRecord record = new PenMatchRecord();
    final List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    val firstBatchRecord = batches.get(0);
    final PenRequestBatchStudentEntity studEntity = firstBatchRecord.getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow();
    studEntity.setLocalID(" " + studEntity.getLocalID() + " " + studEntity.getLocalID().charAt(0) + " ");
    studEntity.setUsualFirstName(studEntity.getLegalFirstName());
    studEntity.setUsualLastName(studEntity.getLegalLastName());
    studEntity.setUsualMiddleNames(studEntity.getLegalMiddleNames());
    studEntity.setGradeCode("12");
    this.penRequestBatchRepository.save(firstBatchRecord);
    record.setStudentID(studEntity.getPenRequestBatchStudentID().toString());
    when(this.restUtils.getStudentByStudentID(studEntity.getPenRequestBatchStudentID().toString()))
        .thenReturn(Student.builder()
            .studentID(studEntity.getPenRequestBatchStudentID().toString())
            .legalFirstName(studEntity.getLegalFirstName())
            .legalLastName(studEntity.getLegalLastName())
            .legalMiddleNames(studEntity.getLegalMiddleNames())
            .usualFirstName(studEntity.getUsualFirstName())
            .usualLastName(studEntity.getUsualLastName())
            .usualMiddleNames(studEntity.getUsualMiddleNames())
            .gradeCode("10")
            .pen(TEST_PEN)
            .build());
    record.setMatchingPEN("123456789");
    eventPayload.setMatchingRecords(new ArrayList<>());
    eventPayload.getMatchingRecords().add(record);
    final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
    doNothing().when(this.restUtils).updateStudent(argument.capture());
    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);

    // now check for student updates if it happened for sys match
    final Student studentUpdate = argument.getValue();
    assertThat(studentUpdate).isNotNull();
    assertThat(studentUpdate.getLocalID()).isNotNull();
    assertThat(studentUpdate.getLocalID()).doesNotContainAnyWhitespaces();
    assertThat(studentUpdate.getUsualLastName()).isNull();
    assertThat(studentUpdate.getUsualMiddleNames()).isNull();
    assertThat(studentUpdate.getUsualFirstName()).isNull();
    if (!firstBatchRecord.getMincode().startsWith("102")) {
      assertThat(studentUpdate.getGradeCode()).isEqualTo("12");
      assertThat(studentUpdate.getGradeYear()).isNotNull();
    } else {
      assertThat(studentUpdate.getGradeCode()).isEqualTo("10");
      assertThat(studentUpdate.getGradeYear()).isNull();
    }

    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    val updatedBatch = this.penRequestBatchRepository.findById(firstBatchRecord.getPenRequestBatchID());
    assertThat(updatedBatch).isPresent();
    assertThat(updatedBatch.get().getPenRequestBatchStudentEntities().stream().filter(entity -> entity.getPenRequestBatchStudentID().equals(studEntity.getPenRequestBatchStudentID())).findFirst().orElseThrow().getPenRequestBatchStudentStatusCode()).isEqualTo(PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode());
  }


  @Test
  public void testProcessPenMatchResult_givenSystemMatchScenarioWithBadLocalID_studentLocalIDShouldBeUpdatedWithNull() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("D1");
    final PenMatchRecord record = new PenMatchRecord();
    final List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    val firstBatchRecord = batches.get(0);
    final PenRequestBatchStudentEntity studEntity = firstBatchRecord.getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow();

    studEntity.setLocalID("N#A");
    this.penRequestBatchRepository.save(firstBatchRecord);
    record.setStudentID(studEntity.getPenRequestBatchStudentID().toString());
    when(this.restUtils.getStudentByStudentID(studEntity.getPenRequestBatchStudentID().toString()))
            .thenReturn(Student.builder()
                    .studentID(studEntity.getPenRequestBatchStudentID().toString())
                    .legalFirstName(studEntity.getLegalFirstName())
                    .legalLastName(studEntity.getLegalLastName())
                    .legalMiddleNames(studEntity.getLegalMiddleNames())
                    .usualFirstName(studEntity.getUsualFirstName())
                    .usualLastName(studEntity.getUsualLastName())
                    .usualMiddleNames(studEntity.getUsualMiddleNames())
                    .gradeCode("10")
                    .pen(TEST_PEN)
                    .build());
    record.setMatchingPEN("123456789");
    eventPayload.setMatchingRecords(new ArrayList<>());
    eventPayload.getMatchingRecords().add(record);
    final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
    doNothing().when(this.restUtils).updateStudent(argument.capture());
    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);

    // now check for student updates if it happened for sys match
    final Student studentUpdate = argument.getValue();
    assertThat(studentUpdate.getLocalID()).isNull();
    // localID after update should not match any of the bad localID
    for (BadLocalID info : EnumSet.allOf(BadLocalID.class)) {
      assertThat(info.getLabel().equals(studentUpdate.getLocalID())).isEqualTo(false);
    }
  }

  @Test
  public void testProcessPenMatchResultForDM() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("DM");
    final PenMatchRecord record = new PenMatchRecord();
    List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    final PenRequestBatchStudentEntity studEntity = batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow();
    record.setStudentID(studEntity.getPenRequestBatchStudentID().toString());
    when(this.restUtils.getStudentByStudentID(studEntity.getPenRequestBatchStudentID().toString())).thenReturn(Student.builder().studentID(studEntity.getPenRequestBatchStudentID().toString()).pen(TEST_PEN).build());
    record.setMatchingPEN("123456789");
    eventPayload.setMatchingRecords(new ArrayList<>());
    eventPayload.getMatchingRecords().add(record);

    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    batches = this.penRequestBatchRepository.findAll();
    assertThat(batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentStatusCode()).isEqualTo(PenRequestBatchStudentStatusCodes.FIXABLE.getCode());
  }

  @Test
  public void testProcessPenMatchResultForD0() throws IOException {
    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));

    prbStudentEntity.setUpdateDate(LocalDateTime.now());
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus("D0");
    List<PenRequestBatchEntity> batches = this.penRequestBatchRepository.findAll();
    final PenRequestBatchStudentEntity studEntity = batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow();
    when(this.restUtils.getStudentByStudentID(studEntity.getPenRequestBatchStudentID().toString())).thenReturn(Student.builder().studentID(studEntity.getPenRequestBatchStudentID().toString()).pen(TEST_PEN).build());
    when(this.restUtils.getNextPenNumberFromPenServiceAPI(this.saga.getSagaId().toString())).thenReturn(TEST_PEN);
    when(this.restUtils.getStudentByPEN(TEST_PEN)).thenReturn(Optional.empty());
    when(this.restUtils.createStudent(this.createStudentCaptor.capture())).thenReturn(Student.builder().studentID(this.studentID).pen(TEST_PEN).build());

    eventPayload.setMatchingRecords(new ArrayList<>());

    this.orchestratorService.processPenMatchResult(this.saga, this.sagaData, eventPayload);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    batches = this.penRequestBatchRepository.findAll();
    assertThat(batches.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow().getPenRequestBatchStudentStatusCode()).isEqualTo(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode());
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

  @Override
  protected String placeholderPenRequestBatchActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"mincode\": \"" + this.mincode + "\",\n" +
        "    \"genderCode\": \"X\",\n" +
        "    \"matchedStudentIDList\": [\"" + this.twinStudentID + "\"]\n" +
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
}
