package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.compare.PenRequestBatchHistoryComparator;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.github.javafaker.Faker;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.Status;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes.STATUS_CHANGED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.DUPLICATE;
import static ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes.K12;
import static ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes.PSI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The type Pen reg batch processor test.
 */
@Slf4j
public class PenRegBatchProcessorTest extends BasePenRegAPITest {
  /**
   * The Min.
   */
  static final int MIN = 1000000;
  /**
   * The Max.
   */
  static final int MAX = 9999999;
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

  @Autowired
  private PenWebBlobRepository penWebBlobRepository;

  /**
   * The Student repository.
   */
  @Autowired
  private PenRequestBatchStudentRepository studentRepository;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  private PenRequestBatchTestUtils penRequestBatchTestUtils;
  /**
   * The Faker.
   */
  Faker faker;

  /**
   * Before.
   */
  @Autowired
  RestUtils restUtils;
  @Before
  public void before() {
    this.faker = new Faker(new Random(0));
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(new School()));
    when(this.restUtils.getPenCoordinator(anyString())).thenReturn(Optional.of(PenCoordinator.builder().penCoordinatorEmail("test@test.com").build()));
    when(this.messagePublisher.requestMessage(anyString(), any())).thenReturn(CompletableFuture.completedFuture(this.getMessage()));
  }


  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given8RowInvalidFileWithHeaderAndRecordLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_8_records_Header_Short_Length.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_8_records_Header_Short_Length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    System.out.println(JsonUtil.getJsonStringFromObject(tsw));
    System.out.println(JsonUtil.getJsonObjectFromString(PENWebBlobEntity.class, JsonUtil.getJsonStringFromObject(tsw)));
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Detail record 1 is missing characters");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).containsIgnoringCase("Detail record 1 is missing characters");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
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
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_10_records_Header_Longer_length.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Header_Longer_length.txt").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getRepeatCount()).isZero();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    assertThat(students.size()).isEqualTo(10);
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthLong_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_10_records_Trailer_Longer_length.txt")).getFile());
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Longer_length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given10RowInvalidFileWithTrailerLengthShort_ShouldCreateLOADFAILRecordsInDB() throws IOException {
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_10_records_Trailer_Shorter_length.txt")).getFile());
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_Trailer_Shorter_length").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFile_ShouldCreateRecordsInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_30_records_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_30_records_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getRepeatCount()).isZero();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    assertThat(students.size()).isEqualTo(30);

    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (final PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  @Test
  @Transactional
  public void testCheckBatchForDuplicateRequests_Given6RowFileWithOneDuplicate_ShouldShowDuplicate() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_Duplicate.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_Duplicate").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getRepeatCount()).isZero();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);

    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    assertThat(students.size()).isEqualTo(6);

    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    var dupCount = 0;
    for (final PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
      if (student.getPenRequestBatchStudentStatusCode().equals(DUPLICATE.getCode())) {
        dupCount++;
      }
    }
    assertThat(dupCount).isEqualTo(1);
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid file should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFileAndExistingRecords_ShouldShowRepeats() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(2);
    final var entity = result.get(1);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    assertThat(entity.getRepeatCount()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(1));
    assertThat(students.stream().filter(s -> PenRequestBatchStudentStatusCodes.REPEAT.getCode().equals(s.getPenRequestBatchStudentStatusCode())).count()).isEqualTo(1);
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (final PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  /**
   * Test that duplicate student request is not marked as a repeat
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenExistingRecordThatIsAlsoDuplicate_ShouldShowDuplicate() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_Duplicate_And_Repeat.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_Duplicate_And_Repeat").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(2);
    final var entity = result.get(1);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getRepeatCount()).isEqualTo(1);
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(1));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    assertThat(students.stream().filter(s -> PenRequestBatchStudentStatusCodes.REPEAT.getCode().equals(s.getPenRequestBatchStudentStatusCode())).count()).isEqualTo(1);
    assertThat(students.stream().filter(s -> DUPLICATE.getCode().equals(s.getPenRequestBatchStudentStatusCode())).count()).isEqualTo(1);
  }

  /**
   * Test process pen reg batch file from tsw given 30 row valid files should create records in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_Given30RowValidFileAndExistingRecords_ShouldShowRepeatsMoreThanOne() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> {
          batch.setSubmissionNumber("T-534094");
          batch.setProcessDate(LocalDateTime.now().minusDays(3));
        });
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(3);
    final var entity = result.get(2);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    assertThat(entity.getRepeatCount()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(2));
    assertThat(students.stream().filter(s -> PenRequestBatchStudentStatusCodes.REPEAT.getCode().equals(s.getPenRequestBatchStudentStatusCode())).count()).isEqualTo(1);
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (final PenRequestBatchStudentEntity student : students) {
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
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_1000_records_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_1000_records_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
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
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_10_records_student_count_mismatch.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_count_mismatch").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid count in trailer record. Stated was 30, Actual was 10");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid count in trailer record. Stated was 30, Actual was 10");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }


  /**
   * Test process pen reg batch file from tsw given record count does not match actual count should create record loadfail in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_BatchToBeHeldBackForSize_ShouldCreateRecordLOADHELDSIZEInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5000_records_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5000_records_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(HOLD_SIZE.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(HOLD_SIZE.getCode());
  }

  /**
   * Test process pen reg batch file from tsw given student record does not start with srm should create record loadfail in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenStudentRecordDoesNotStartWithSRM_ShouldCreateRecordLOADFAILInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_10_records_student_does_not_start_with_SRM_mismatch.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_10_records_student_does_not_start_with_SRM_mismatch").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid transaction code on Detail record");
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).containsIgnoringCase("Invalid transaction code on Detail record");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  /**
   * Test process pen reg batch file from tsw given min code starts with 102 should create record loaded in db.
   *
   * @throws IOException the io exception
   */
  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_PSI_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("10210518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_PSI_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(PSI.getCode());
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}",students);
    var counter = 1;
    for (final PenRequestBatchStudentEntity student : students) {
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
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeDoesNotStartsWith102_ShouldCreateRecordLOADEDInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(REPEATS_CHECKED.getCode());
    assertThat(entity.getSchoolGroupCode()).isEqualTo(K12.getCode());
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(2);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isNull();
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isEqualTo(5);
    students.sort(Comparator.comparing(PenRequestBatchStudentEntity::getRecordNumber));
    log.error("students {}", students);
    var counter = 1;
    for (final PenRequestBatchStudentEntity student : students) {
      assertThat(counter++).isEqualTo(student.getRecordNumber());
    }
  }

  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeInvalid_ShouldCreateRecordLOADFAILInDB() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.empty());
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getSchoolGroupCode()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isEqualTo("Invalid Mincode in Header record.");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeInvalidSchoolCloseDate_ShouldCreateRecordLOADFAILInDB() throws IOException {
    final School school = this.createMockSchool();
    school.setDateClosed("1996-09-01T00:00:00");
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getSchoolGroupCode()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isEqualTo("Invalid Mincode in Header record - school is closed.");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeInvalidSchoolOpenDate_ShouldCreateRecordLOADFAILInDB() throws IOException {
    final School school = this.createMockSchool();
    school.setDateOpened("2024-09-01T00:00:00");
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getSchoolGroupCode()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isEqualTo("Invalid Mincode in Header record - school is closed.");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  @Test
  @Transactional
  public void testProcessPenRegBatchFileFromTSW_GivenmincodeInvalidSchoolOpenDateFormat_ShouldCreateRecordLOADFAILInDB() throws IOException {
    final School school = this.createMockSchool();
    school.setDateOpened("88888888");
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
    final byte[] bFile = Files.readAllBytes(file.toPath());
    final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
    var tsw = PENWebBlobEntity.builder().penWebBlobId(1L).mincode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    tsw = this.penRequestBatchTestUtils.savePenWebBlob(tsw);
    this.penRegBatchProcessor.processPenRegBatchFileFromPenWebBlob(tsw);
    final var result = this.repository.findAll();
    assertThat(result.size()).isEqualTo(1);
    final var entity = result.get(0);
    assertThat(entity.getPenRequestBatchID()).isNotNull();
    assertThat(entity.getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(entity.getSchoolGroupCode()).isNull();
    assertThat(entity.getPenRequestBatchHistoryEntities().size()).isEqualTo(1);
    final Optional<PenRequestBatchHistoryEntity> penRequestBatchHistoryEntityOptional = entity.getPenRequestBatchHistoryEntities().stream().min(new PenRequestBatchHistoryComparator());
    assertThat(penRequestBatchHistoryEntityOptional).isPresent();
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchEventCode()).isEqualTo(STATUS_CHANGED.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusCode()).isEqualTo(LOAD_FAIL.getCode());
    assertThat(penRequestBatchHistoryEntityOptional.get().getPenRequestBatchStatusReason()).isEqualTo("Invalid Mincode in Header record - school is closed.");
    final var students = this.studentRepository.findAllByPenRequestBatchEntity(result.get(0));
    assertThat(students.size()).isZero();
  }

  private School createMockSchool() {
    final School school = new School();
    school.setSchoolName("Marco's school");
    school.setMincode("66510518");
    school.setDateOpened("1964-09-01T00:00:00");
    return school;
  }

  private Message getMessage() {
    return new Message() {
      @Override
      public String getSubject() {
        return null;
      }

      @Override
      public String getReplyTo() {
        return null;
      }

      @Override
      public boolean hasHeaders() {
        return false;
      }

      @Override
      public Headers getHeaders() {
        return null;
      }

      @Override
      public boolean isStatusMessage() {
        return false;
      }

      @Override
      public Status getStatus() {
        return null;
      }

      @Override
      public byte[] getData() {
        return new byte[1];
      }

      @Override
      public boolean isUtf8mode() {
        return false;
      }

      @Override
      public Subscription getSubscription() {
        return null;
      }

      @Override
      public String getSID() {
        return null;
      }

      @Override
      public Connection getConnection() {
        return null;
      }

      @Override
      public NatsJetStreamMetaData metaData() {
        return null;
      }

      @Override
      public void ack() {

      }

      @Override
      public void ackSync(Duration timeout) {

      }

      @Override
      public void nak() {

      }

      @Override
      public void term() {

      }

      @Override
      public void inProgress() {

      }

      @Override
      public boolean isJetStream() {
        return false;
      }
    };
  }


}

