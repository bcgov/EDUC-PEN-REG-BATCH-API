package ca.bc.gov.educ.penreg.api.batch.service;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.ARCHIVED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PenRequestBatchFileServiceTest extends BasePenRegAPITest {
  @Autowired
  RestUtils restUtils;
  /**
   * The Repository.
   */
  @Autowired
  private PenRequestBatchRepository repository;
  /**
   * The Student repository.
   */
  @Autowired
  private PenRequestBatchTestUtils penRequestBatchTestUtils;
  @Autowired
  private PenRequestBatchFileService penRequestBatchFileService;

  @Before
  public void before() {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(new School()));
  }


  @Test
  public void testFilterDuplicatesAndRepeatRequests_givenRepeatedStudentsInAFile_shouldRemoveRepeatedStudentsFromReturnedSet() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    this.penRequestBatchTestUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_dup_rpt_chk.json",
        "mock_pen_req_batch_student_repeat_2.json", 1, (batch) -> batch.setPenRequestBatchStatusCode("LOADED"));
    final var result = this.repository.findBySubmissionNumber("T-534095");
    assertThat(result).isNotEmpty();
    val filteredSet = this.penRequestBatchFileService.filterDuplicatesAndRepeatRequests(UUID.randomUUID().toString(),
      result.get(0));
    assertThat(filteredSet.size()).isZero();

  }

  @Test
  public void testFilterDuplicatesAndRepeatRequests_givenRepeatedStudentsInALargeFile_shouldRemoveRepeatedStudentsFromReturnedSet() throws IOException, FileUnProcessableException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    //sample_5000_records_OK
    final String submissionNumber = this.penRequestBatchTestUtils.createBatchStudentsFromFile("sample_5000_records_OK" +
      ".txt", ARCHIVED.getCode());
    final String submissionNumber2 = this.penRequestBatchTestUtils.createBatchStudentsFromFile("sample_5000_records_OK" +
      ".txt", LOADED.getCode());
    val previousBatch = this.repository.findBySubmissionNumber(submissionNumber);
    assertThat(previousBatch).isNotEmpty();
    val prvbatchEntity = previousBatch.get(0);
    prvbatchEntity.setPenRequestBatchStatusCode(ARCHIVED.getCode());
    prvbatchEntity.setProcessDate(LocalDateTime.now().minusDays(2));
    this.penRequestBatchTestUtils.updateBatchInNewTransaction(prvbatchEntity);
    final var result = this.repository.findBySubmissionNumber(submissionNumber2);
    assertThat(result).isNotEmpty();
    val filteredSet = this.penRequestBatchFileService.filterDuplicatesAndRepeatRequests(UUID.randomUUID().toString(),
      result.get(0));
    assertThat(filteredSet.size()).isZero();

  }

  @Test
  public void testFilterDuplicatesAndRepeatRequests_givenSFASFileAndRepeats_shouldRemoveNotRepeatedStudentsFromReturnedSet() throws IOException, FileUnProcessableException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    //sample_5000_records_OK
    final String submissionNumber = this.penRequestBatchTestUtils.createBatchStudentsFromFile("sample_5000_records_OK" +
      ".txt", ARCHIVED.getCode());
    final String submissionNumber2 = this.penRequestBatchTestUtils.createBatchStudentsFromFile("sample_5000_records_OK" +
      ".txt", LOADED.getCode());
    val previousBatch = this.repository.findBySubmissionNumber(submissionNumber);
    assertThat(previousBatch).isNotEmpty();
    val prvbatchEntity = previousBatch.get(0);
    prvbatchEntity.setPenRequestBatchStatusCode(ARCHIVED.getCode());
    prvbatchEntity.setProcessDate(LocalDateTime.now().minusDays(2));
    this.penRequestBatchTestUtils.updateBatchInNewTransaction(prvbatchEntity);
    final var result = this.repository.findBySubmissionNumber(submissionNumber2);
    assertThat(result).isNotEmpty();
    result.get(0).setMincode("10200030");
    val filteredSet = this.penRequestBatchFileService.filterDuplicatesAndRepeatRequests(UUID.randomUUID().toString(),
      result.get(0));
    assertThat(filteredSet.size()).isEqualTo(5000);

  }


  private School createMockSchool() {
    final School school = new School();
    school.setSchoolName("Marco's school");
    school.setMincode("66510518");
    school.setDateOpened("1964-09-01T00:00:00");
    return school;
  }
}
