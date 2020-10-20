package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes.STATUS_CHANGED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.*;
import static ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes.K12;
import static ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes.PSI;
import static ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils.createBatchStudents;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Pen reg batch processor test.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PenRegBatchProcessorTest {
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  /**
   * The Min.
   */
  static final int MIN = 1000000;
  /**
   * The Max.
   */
  static final int MAX = 9999999;
  /**
   * The Grade codes.
   */
  private final String[] gradeCodes = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "SU", "GA"};
  /**
   * The Postal codes.
   */
  private final String[] postalCodes = {"V8N0A1", "V8N0A2", "V8N0A3", "V8N0A4", "V8N0A5", "V8N0A6", "V8N0A7", "V8N0A8", "V8N0A9", "V8N0B1",
      "V8N0B2", "V8N0B3", "V8N0B4", "V8N0B5", "V8N0B6", "V8N0B7", "V8N1A1", "V8N1A2", "V8N1A3", "V8N1A4", "V8N1A5", "V8N1A6",
      "V8N1A7", "V8N1A8", "V8N1A9", "V8N1B3", "V8N1B4", "V8N1B5", "V8N1B6", "V8N1B7", "V8N1B8", "V8N1B9", "V8N1C1", "V8N1C2",
      "V8N1C3", "V8N1C4", "V8N1C5", "V8N1C6", "V8N1C8", "V8N1C9", "V8N1E1", "V8N1E2", "V8N1E3", "V8N1E4", "V8N1E5", "V8N1E6",
      "V8N1E8", "V8N1E9", "V8N1G1", "V8N1G2", "V8N1G3", "V8N1G4", "V8N1G5", "V8N1G6", "V8N1G7", "V8N1G8", "V8N1G9", "V8N1H1",
      "V8N1H2", "V8N1H3", "V8N1H4", "V8N1H5", "V8N1H6", "V8N1H7", "V8N1H8", "V8N1H9", "V8N1J1", "V8N1J2", "V8N1J3", "V8N1J4",
      "V8N1J5", "V8N1J6", "V8N1J7", "V8N1J8", "V8N1J9", "V8N1K1", "V8N1K2", "V8N1K3", "V8N1K4", "V8N1K5", "V8N1K6", "V8N1K7",
      "V8N1K8", "V8N1K9", "V8N1L1", "V8N1L2", "V8N1L3"};
  /**
   * The Pen reg batch processor.
   */
  @Autowired
  private PenRegBatchProcessor penRegBatchProcessor;
  /**
   * The Repository.
   */
  @Autowired
  private PenRequestBatchRepository repository;

  /**
   * The Student repository.
   */
  @Autowired
  private PenRequestBatchStudentRepository studentRepository;

  /**
   * The Pen web blob repository.
   */
  @Autowired
  private PenWebBlobRepository penWebBlobRepository;
  /**
   * The Faker.
   */
  Faker faker;

  /**
   * Before.
   */
  @Before
  public void before() {
    faker = new Faker(new Random(0));
  }


  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given8RowInvalidFileWithHeaderLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_8_records_Header_Short_Length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_8_records_Header_Short_Length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    System.out.println(JsonUtil.getJsonStringFromObject(tsw));
    System.out.println(JsonUtil.getJsonObjectFromString(PENWebBlobEntity.class, JsonUtil.getJsonStringFromObject(tsw)));
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Header record is missing characters");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Header record is missing characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithHeaderLengthLong_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Header_Longer_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Header_Longer_length.txt").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Header record has extraneous characters");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Header record has extraneous characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthLong_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Trailer_Longer_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Longer_length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Trailer record has extraneous characters");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Trailer record has extraneous characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Trailer_Shorter_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Shorter_length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Trailer record is missing characters");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Trailer record is missing characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFile_ShouldCreateRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_30_records_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_30_records_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getRepeatCount()).isZero();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).isNull();
    assertThat(students.size()).isEqualTo(30);

    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFileAndExistingRecords_ShouldShowRepeats() throws IOException {
    createBatchStudents(repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
      (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(2);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    assertThat(entity.getRepeatCount()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).isNull();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.stream().filter(s -> PenRequestBatchStudentStatusCodes.REPEAT.getCode().equals(s.getPenRequestBatchStudentStatusCode())).count()).isEqualTo(1);
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  /**
   * Test process pen reg batch file from tsw given 10000 row file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given1000RowFile_ShouldCreateRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_1000_records_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_1000_records_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).isNull();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(1000);
  }

  /**
   * Test process pen reg batch file from tsw given record count does not match actual count should create record loadfail in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenRecordCountDoesNotMatchActualCount_ShouldCreateRecordLOADFAILInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_student_count_mismatch.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_count_mismatch").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid count in trailer record. Stated was 30, Actual was 10");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Invalid count in trailer record. Stated was 30, Actual was 10");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given student record does not start with srm should create record loadfail in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenStudentRecordDoesNotStartWithSRM_ShouldCreateRecordLOADFAILInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_student_does_not_start_with_SRM_mismatch.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_does_not_start_with_SRM_mismatch").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid transaction code on Detail record");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).containsIgnoringCase("Invalid transaction code on Detail record");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given min code starts with 102 should create record loaded in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenMinCodeStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_PSI_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_PSI_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(PSI.getCode());
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).isNull();
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  /**
   * Test process pen reg batch file from tsw given min code does not starts with 102 should create record loaded in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenMinCodeDoesNotStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().findFirst();
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getEventReason()).isNull();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  /**
   * After.
   */
  @After
  @Transactional
  public void after() {
    repository.deleteAll();
    penWebBlobRepository.deleteAll();
  }

  /**
   * Generate header record string.
   *
   * @return the string
   */
  private String generateHeaderRecord() {
    return "FFI61610518 Braefoot Elementary                     20200707kleahy@sd61.bc.ca                                                                                   2504779616School Contact";
  }

  /**
   * Generate invalid header record string.
   *
   * @return the string
   */
  private String generateInvalidHeaderRecord() {
    return "FFX61610518 Braefoot Elementary                     20200707kleahy@sd61.bc.ca                                                                                   2504779616School Contact";
  }

  /**
   * Generate trailer record string.
   *
   * @param numOfStudents the num of students
   * @return the string
   */
  private String generateTrailerRecord(int numOfStudents) {
    return "BTR" + StringUtils.rightPad(String.valueOf(numOfStudents), 6, " ") + "    Vendor Name                                                                                         Product Name                                                                                        Product ID";
  }

  /**
   * Generate invalid trailer record string.
   *
   * @param numOfStudents the num of students
   * @return the string
   */
  private String generateInvalidTrailerRecord(int numOfStudents) {
    return "BTR" + numOfStudents + "    Vendor Name                                                                                         Product Name                                                                                        Product ID";
  }

  /**
   * Generate random student record string.
   *
   * @param localID the local id
   * @return the string
   */
  private String generateRandomStudentRecord(String localID) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    Random random = new Random();
    String gender = "M";
    if (random.nextBoolean()) {
      gender = "F";
    }
    int randomGradeIndex = (int) Math.floor(Math.random() * gradeCodes.length);
    String randomGrade = gradeCodes[randomGradeIndex];
    int randomPostalCodeIndex = (int) Math.floor(Math.random() * postalCodes.length);
    String randomPostalCode = postalCodes[randomPostalCodeIndex];
    return "SRM"
        .concat(localID)
        .concat(StringUtils.leftPad("", 10, " "))
        .concat(StringUtils.rightPad(faker.name().lastName(), 25, " "))
        .concat(StringUtils.rightPad(faker.name().firstName(), 25, " "))
        .concat(StringUtils.rightPad("", 25, " "))
        .concat(StringUtils.rightPad(faker.name().lastName(), 25, " "))
        .concat(StringUtils.rightPad(faker.name().firstName(), 25, " "))
        .concat(StringUtils.rightPad("", 25, " "))
        .concat(StringUtils.rightPad(dateFormat.format(faker.date().birthday()), 8, " "))
        .concat(gender)
        .concat(StringUtils.leftPad("", 16, " "))
        .concat(randomGrade)
        .concat(StringUtils.leftPad("", 26, " "))
        .concat(randomPostalCode);
  }
}
