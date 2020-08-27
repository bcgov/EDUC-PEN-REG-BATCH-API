package ca.bc.gov.educ.penreg.api.batch.schedulers;

import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Pen reg batch scheduler test.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Slf4j
public class PenRegBatchSchedulerTest {
  /**
   * The Min.
   */
  static final int MIN = 1000000;
  /**
   * The Max.
   */
  static final int MAX = 9999999;
  /**
   * The Pen reg batch scheduler.
   */
  @Autowired
  private PenRegBatchScheduler penRegBatchScheduler;
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
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    List<PENWebBlobEntity> entities = createDummyRecords();
    penWebBlobRepository.saveAll(entities);
  }

  /**
   * Create dummy records list.
   *
   * @return the list
   * @throws IOException the io exception
   */
  private List<PENWebBlobEntity> createDummyRecords() throws IOException {
    List<PENWebBlobEntity> entities = new ArrayList<>();
    for (var index = 0; index < 2; index++) {
      entities.add(createDummyRecord(index));
    }
    return entities;
  }

  /**
   * Create dummy record pen web blob entity.
   *
   * @param index the index
   * @return the pen web blob entity
   * @throws IOException the io exception
   */
  private PENWebBlobEntity createDummyRecord(int index) throws IOException {
    if (index == 0) {
      var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
      File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_K12_OK.txt")).getFile());
      byte[] bFile = Files.readAllBytes(file.toPath());
      return PENWebBlobEntity.builder().penWebBlobId(1L).studentCount(5L).minCode("66510518").sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_K12_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    } else {
      var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);
      File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample_5_PSI_OK.txt")).getFile());
      byte[] bFile = Files.readAllBytes(file.toPath());
      return PENWebBlobEntity.builder().penWebBlobId(2L).minCode("10210518").studentCount(5L).sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName("sample_5_PSI_OK").fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(("T" + randomNum).substring(0, 8)).build();
    }
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    penWebBlobRepository.deleteAll();
    repository.deleteAll();
  }

  /**
   * Test extract un processed files from tsw given rows in ts with extract date null should be processed.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExtractUnProcessedFilesFromTSW_GivenRowsInTSWithExtractDateNull_ShouldBeProcessed() throws InterruptedException {
    penRegBatchScheduler.extractUnProcessedFilesFromPenWebBlobs();
    assertThat(studentRepository.findAll().size()).isEqualTo(10);
  }
}
