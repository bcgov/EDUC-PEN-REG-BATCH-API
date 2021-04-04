package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.BaseTest;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeSoftDeletedBatchRecordsSchedulerTest extends BaseTest {

  @Autowired
  PenRequestBatchRepository penRequestBatchRepository;

  @Autowired
  PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  @Autowired
  PenRequestBatchHistoryRepository penRequestBatchHistoryRepository;

  @Autowired
  PurgeSoftDeletedBatchRecordsScheduler purgeSoftDeletedBatchRecordsScheduler;

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchHistoryMapper historyMapper = PenRequestBatchHistoryMapper.mapper;


  @Test
  public void pollBatchFilesAndPurgeSoftDeletedRecords_givenOldRecordsPresent_shouldBeDeleted() throws IOException {
    final List<PenRequestBatchEntity> prbEntities = this.createBatchStudents(2);

    final var softDeletedPrbEntity =  prbEntities.get(0);
    assertThat(softDeletedPrbEntity.getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.DELETED.getCode());
    final var softDeletedPrb = mapper.toStructure(softDeletedPrbEntity);
    final var prbHistoryEntity = historyMapper.toModel(softDeletedPrb);
    prbHistoryEntity.setPenRequestBatchEntity(softDeletedPrbEntity);
    prbHistoryEntity.setCreateDate(LocalDateTime.now().minusDays(3));
    prbHistoryEntity.setUpdateDate(LocalDateTime.now().minusDays(3));
    prbHistoryEntity.setEventDate(LocalDateTime.now().minusDays(3));
    prbHistoryEntity.setPenRequestBatchEventCode(PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
    this.penRequestBatchHistoryRepository.save(prbHistoryEntity);

    this.purgeSoftDeletedBatchRecordsScheduler.setSoftDeletedBatchRecordsRetentionDays(2);
    this.purgeSoftDeletedBatchRecordsScheduler.pollBatchFilesAndPurgeSoftDeletedRecords();

    // check prb histories
    final var prbHistories = this.penRequestBatchHistoryRepository.findAll();
    assertThat(prbHistories).isEmpty();

    // check prb students
    final var prbStudents = this.penRequestBatchStudentRepository.findAll();
    assertThat(prbStudents.size()).isPositive();
    prbStudents.forEach(st -> assertThat(st.getPenRequestBatchEntity().getPenRequestBatchStatusCode()).isNotEqualTo(PenRequestBatchStatusCodes.DELETED.getCode()));

    // check prb files
    final var prbFiles = this.penRequestBatchRepository.findAll();
    assertThat(prbFiles.size()).isEqualTo(prbEntities.size() - 1);
    prbFiles.forEach(prb -> assertThat(prb.getPenRequestBatchStatusCode()).isNotEqualTo(PenRequestBatchStatusCodes.DELETED.getCode()));
  }

  /**
   * Create batch students list.
   *
   * @param total the total
   * @return the list
   * @throws IOException the io exception
   */
  private List<PenRequestBatchEntity> createBatchStudents(final Integer total) throws IOException {
    return PenRequestBatchUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch.json",
            "mock_pen_req_batch_student.json", total,
            (batch) -> {
              if (batch.getSubmissionNumber().equals("T-534093")) {
                batch.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.DELETED.getCode());
                batch.setCreateDate(LocalDateTime.now().minusDays(3));
              }
            });
  }
}
