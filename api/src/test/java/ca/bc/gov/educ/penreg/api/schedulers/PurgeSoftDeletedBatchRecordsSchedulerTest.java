package ca.bc.gov.educ.penreg.api.schedulers;

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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PurgeSoftDeletedBatchRecordsSchedulerTest {

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
    List<PenRequestBatchEntity> prbEntities = createBatchStudents(4);
    prbEntities.forEach(prbEntity -> {
      final var prb = mapper.toStructure(prbEntity);
      final var prbHistoryEntity = historyMapper.toModel(prb);
      prbHistoryEntity.setPenRequestBatchEntity(prbEntity);
      prbHistoryEntity.setCreateDate(LocalDateTime.now().minusDays(3));
      prbHistoryEntity.setUpdateDate(LocalDateTime.now().minusDays(3));
      prbHistoryEntity.setEventDate(LocalDateTime.now().minusDays(3));
      prbHistoryEntity.setPenRequestBatchEventCode(PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
      penRequestBatchHistoryRepository.save(prbHistoryEntity);
    });
    this.purgeSoftDeletedBatchRecordsScheduler.setSoftDeletedBatchRecordsRetentionDays(2);
    this.purgeSoftDeletedBatchRecordsScheduler.pollBatchFilesAndPurgeSoftDeletedRecords();

    // check prb histories
    final var prbHistories = this.penRequestBatchHistoryRepository.findAll();
    assertThat(prbHistories).isEmpty();

    // check prb students
    final var prbStudents = this.penRequestBatchStudentRepository.findAll();
    assertThat(prbStudents).isEmpty();

    // check prb files
    final var prbFiles = this.penRequestBatchRepository.findAll();
    assertThat(prbFiles).isEmpty();
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
              batch.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.DELETED.getCode());
              batch.setCreateDate(LocalDateTime.now().minusDays(3));
            });
  }
}
