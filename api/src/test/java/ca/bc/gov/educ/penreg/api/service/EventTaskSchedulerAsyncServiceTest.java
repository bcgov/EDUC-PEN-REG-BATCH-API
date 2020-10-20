package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import static ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils.createBatchStudents;
import static ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils.createSagaRecords;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Slf4j
public class EventTaskSchedulerAsyncServiceTest {
  @Autowired
  private SagaRepository sagaRepository;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  private EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    penRequestBatchRepository.deleteAll();
    sagaRepository.deleteAll();
  }


  @Test
  public void testMarkProcessedBatchesActive_GivenNewPen_ShouldUpdateCountInDB() throws IOException {
    var batches = createBatchStudents(penRequestBatchRepository, "mock_pen_req_batch_repeats_checked.json",
      "mock_pen_req_batch_student_repeats_checked.json", 1);
    createSagaRecords(sagaRepository, batches);
    eventTaskSchedulerAsyncService.markProcessedBatchesActive();
    var batch = penRequestBatchRepository.findById(batches.get(0).getPenRequestBatchID());
    assertThat(batch.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.ACTIVE.getCode());
    assertThat(batch.get().getErrorCount()).isEqualTo(1);
    assertThat(batch.get().getFixableCount()).isEqualTo(1);
    assertThat(batch.get().getMatchedCount()).isEqualTo(1);
    assertThat(batch.get().getNewPenCount()).isEqualTo(2);
  }


}
