package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;

import static ca.bc.gov.educ.penreg.api.batch.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.batch.constants.PenRequestBatchStatusCodes.LOAD_FAIL;
import static ca.bc.gov.educ.penreg.api.batch.constants.SchoolGroupCodes.K12;
import static ca.bc.gov.educ.penreg.api.batch.constants.SchoolGroupCodes.PSI;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Pen reg batch processor test.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class PenRegBatchProcessorTest {
  /**
   * The Min.
   */
  static final int MIN = 1000000;
  /**
   * The Max.
   */
  static final int MAX = 9999999;
  private final String[] gradeCodes = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "SU", "GA"};
  private final String[] postalCodes = {"V8N0A1", "V8N0A2", "V8N0A3", "V8N0A4", "V8N0A5", "V8N0A6", "V8N0A7", "V8N0A8", "V8N0A9", "V8N0B1",
      "V8N0B2", "V8N0B3", "V8N0B4", "V8N0B5", "V8N0B6", "V8N0B7", "V8N1A1", "V8N1A2", "V8N1A3", "V8N1A4", "V8N1A5", "V8N1A6",
      "V8N1A7", "V8N1A8", "V8N1A9", "V8N1B3", "V8N1B4", "V8N1B5", "V8N1B6", "V8N1B7", "V8N1B8", "V8N1B9", "V8N1C1", "V8N1C2",
      "V8N1C3", "V8N1C4", "V8N1C5", "V8N1C6", "V8N1C8", "V8N1C9", "V8N1E1", "V8N1E2", "V8N1E3", "V8N1E4", "V8N1E5", "V8N1E6",
      "V8N1E8", "V8N1E9", "V8N1G1", "V8N1G2", "V8N1G3", "V8N1G4", "V8N1G5", "V8N1G6", "V8N1G7", "V8N1G8", "V8N1G9", "V8N1H1",
      "V8N1H2", "V8N1H3", "V8N1H4", "V8N1H5", "V8N1H6", "V8N1H7", "V8N1H8", "V8N1H9", "V8N1J1", "V8N1J2", "V8N1J3", "V8N1J4",
      "V8N1J5", "V8N1J6", "V8N1J7", "V8N1J8", "V8N1J9", "V8N1K1", "V8N1K2", "V8N1K3", "V8N1K4", "V8N1K5", "V8N1K6", "V8N1K7",
      "V8N1K8", "V8N1K9", "V8N1L1", "V8N1L2", "V8N1L3"};
  @Autowired
  private PenRegBatchProcessor penRegBatchProcessor;
  @Autowired
  private PenRequestBatchRepository repository;

  @Autowired
  private PenRequestBatchStudentRepository studentRepository;

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
  public void testProcessPenRegBatchFileFromTSW_Given8RowInvalidFileWithHeaderLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_8_records_Header_Short_Length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_8_records_Header_Short_Length").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Header record is missing characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithHeaderLengthLong_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Header_Longer_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Header_Longer_length.txt").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Header record has extraneous characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthLong_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Trailer_Longer_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Longer_length").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Trailer record has extraneous characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_Trailer_Shorter_length.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Shorter_length").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Trailer record is missing characters");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFile_ShouldCreateRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_30_records_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_30_records_OK").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(30);
    log.info("PenRequestBatch Entity is :: {}", entity);
  }

  /**
   * Test process pen reg batch file from tsw given 10000 row file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  public void testProcessPenRegBatchFileFromTSW_Given1000RowFile_ShouldCreateRecordsInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_1000_records_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_1000_records_OK").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(1000);
  }

  @Test
  public void testProcessPenRegBatchFileFromTSW_GivenRecordCountDoesNotMatchActualCount_ShouldCreateRecordLOADFAILInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_student_count_mismatch.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_count_mismatch").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid count in trailer record. Stated was 30, Actual was 10");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  @Test
  public void testProcessPenRegBatchFileFromTSW_GivenStudentRecordDoesNotStartWithSRM_ShouldCreateRecordLOADFAILInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_10_records_student_does_not_start_with_SRM_mismatch.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_does_not_start_with_SRM_mismatch").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid transaction code on Detail record");
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  @Test
  public void testProcessPenRegBatchFileFromTSW_GivenMinCodeStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_PSI_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_PSI_OK").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(PSI.getCode());
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(5);
  }
  @Test
  public void testProcessPenRegBatchFileFromTSW_GivenMinCodeDoesNotStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("txt").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    var result = repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOADED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    var students = studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(5);
  }

  /**
   * After.
   */
  @After
  public void after() {
    repository.deleteAll();
    penWebBlobRepository.deleteAll();
  }

  private String generateHeaderRecord() {
    return "FFI61610518 Braefoot Elementary                     20200707kleahy@sd61.bc.ca                                                                                   2504779616School Contact";
  }

  private String generateInvalidHeaderRecord() {
    return "FFX61610518 Braefoot Elementary                     20200707kleahy@sd61.bc.ca                                                                                   2504779616School Contact";
  }

  private String generateTrailerRecord(int numOfStudents) {
    return "BTR" + StringUtils.rightPad(String.valueOf(numOfStudents), 6, " ") + "    Vendor Name                                                                                         Product Name                                                                                        Product ID";
  }

  private String generateInvalidTrailerRecord(int numOfStudents) {
    return "BTR" + numOfStudents + "    Vendor Name                                                                                         Product Name                                                                                        Product ID";
  }

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