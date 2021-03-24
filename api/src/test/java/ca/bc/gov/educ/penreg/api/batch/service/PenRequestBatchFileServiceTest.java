package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.School;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PenRequestBatchFileServiceTest {
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
  private PenRequestBatchStudentRepository studentRepository;
  @Autowired
  private PenRequestBatchHistoryRepository penRequestBatchHistoryRepository;
  /**
   * The Pen web blob repository.
   */
  @Autowired
  private PenWebBlobRepository penWebBlobRepository;
  @Autowired
  private PenRequestBatchUtils penRequestBatchUtils;
  @Autowired
  private PenRequestBatchFileService penRequestBatchFileService;

  @Before
  public void before() {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(new School()));
  }

  @Test
  public void testFilterDuplicatesAndRepeatRequests_givenRepeatedStudentsInAFile_shouldRemoveRepeatedStudentsFromReturnedSet() throws IOException {
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool()));
    this.penRequestBatchUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_repeat.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.now().minusDays(3)));
    this.penRequestBatchUtils.createBatchStudentsInSingleTransaction(this.repository, "mock_pen_req_batch_dup_rpt_chk.json",
        "mock_pen_req_batch_student_repeat_2.json", 1, (batch) -> batch.setPenRequestBatchStatusCode("LOADED"));
    final var result = this.repository.findBySubmissionNumber("T-534095");
    assertThat(result).isPresent();
    val filteredSet = this.penRequestBatchFileService.filterDuplicatesAndRepeatRequests(UUID.randomUUID().toString(),
        result.get());
    assertThat(filteredSet.size()).isZero();

  }

  private School createMockSchool() {
    final School school = new School();
    school.setSchoolName("Marco's school");
    school.setMincode("66510518");
    school.setDateOpened("1964-09-01T00:00:00");
    return school;
  }
}
