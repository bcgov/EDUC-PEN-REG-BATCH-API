package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REPEATS_CHECKED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA;
import static ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils.createBatchStudents;
import static ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils.createSagaRecords;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class EventTaskSchedulerAsyncServiceTest extends BasePenRegAPITest {
  @Autowired
  private SagaRepository sagaRepository;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  private PenRequestBatchHistoryRepository historyRepository;

  @Autowired
  private EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;



  @Test
  public void testMarkProcessedBatchesActive_GivenNewPen_ShouldUpdateCountInDB() throws IOException {
    final var batches = createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_repeats_checked.json",
        "mock_pen_req_batch_student_repeats_checked.json", 1);
    createSagaRecords(this.sagaRepository, batches);
    this.eventTaskSchedulerAsyncService.markProcessedBatchesActive();

    // history records.
    final var batch = this.penRequestBatchRepository.findById(batches.get(0).getPenRequestBatchID());
    assertThat(batch).isPresent();
    assertThat(batch.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.ACTIVE.getCode());
    assertThat(batch.get().getErrorCount()).isEqualTo(1);
    assertThat(batch.get().getFixableCount()).isEqualTo(1);
    assertThat(batch.get().getMatchedCount()).isEqualTo(1);
    assertThat(batch.get().getNewPenCount()).isEqualTo(2);
    val batchEntity = batch.get();
    batchEntity.setPenRequestBatchStatusCode(REPEATS_CHECKED.getCode());
    this.penRequestBatchRepository.save(batchEntity);
    this.eventTaskSchedulerAsyncService.markProcessedBatchesActive(); //update to repeat and  call it twice to verify
    // there are no two history records to indicate it was not processed twice
    val historyEntities = this.historyRepository.findAllByPenRequestBatchEntity(batch.get());
    assertThat(historyEntities).isNotEmpty().size().isEqualTo(1);
  }

  @Test
  public void testMarkProcessedBatchesActive_GivenAllNewPenAndMatched_ShouldUpdateCountInDB() throws IOException {
    final var batches = createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_repeats_checked.json",
      "mock_pen_req_batch_student_all_newpen_and_macthed.json", 1);
    createSagaRecords(this.sagaRepository, batches);
    this.eventTaskSchedulerAsyncService.markProcessedBatchesActive();

    // history records.
    final var batch = this.penRequestBatchRepository.findById(batches.get(0).getPenRequestBatchID());
    assertThat(batch).isPresent();
    assertThat(batch.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.ARCHIVED.getCode());
    assertThat(batch.get().getErrorCount()).isEqualTo(0);
    assertThat(batch.get().getFixableCount()).isEqualTo(0);
    assertThat(batch.get().getMatchedCount()).isEqualTo(3);
    assertThat(batch.get().getNewPenCount()).isEqualTo(2);
    val historyEntities = this.historyRepository.findAllByPenRequestBatchEntity(batch.get());
    assertThat(historyEntities).isNotEmpty().size().isEqualTo(1);

    var saga = this.sagaRepository.findByPenRequestBatchIDAndSagaName(batch.get().getPenRequestBatchID(), PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA.toString());
    assertThat(saga).isNotEmpty().size().isEqualTo(1);
  }
}
