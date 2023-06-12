package ca.bc.gov.educ.penreg.api.schedulers;

import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PurgeSoftDeletedBatchRecordsScheduler {
  @Getter(PRIVATE)
  private final PenRequestBatchRepository penRequestBatchRepository;

  @Value("${soft.deleted.batch.records.retention.days}")
  @Setter
  @Getter
  Integer softDeletedBatchRecordsRetentionDays;

  public PurgeSoftDeletedBatchRecordsScheduler(final PenRequestBatchRepository penRequestBatchRepository) {
    this.penRequestBatchRepository = penRequestBatchRepository;
  }

  /**
   * run the job based on configured scheduler(a cron expression) and purge old soft deleted batch records from DB.
   */
  @Scheduled(cron = "${scheduled.jobs.purge.soft.deleted.batch.records.cron}")
  @SchedulerLock(name = "PurgeSoftDeletedBatchRecordsLock",
      lockAtLeastFor = "PT1H", lockAtMostFor = "PT1H") //midnight job so lock for an hour
  @Transactional
  public void pollBatchFilesAndPurgeSoftDeletedRecords() {
    LockAssert.assertLocked();
    final LocalDateTime createDateToCompare = this.calculateCreateDateBasedOnSoftDeletedRetentionDays();
    final var prbRecords = this.getPenRequestBatchRepository().findByPenRequestBatchStatusCodeAndCreateDateBefore(
      PenRequestBatchStatusCodes.DELETED.getCode(), createDateToCompare);
    if (!prbRecords.isEmpty()) {
      this.getPenRequestBatchRepository().deleteAll(prbRecords);
    }
  }

  private LocalDateTime calculateCreateDateBasedOnSoftDeletedRetentionDays() {
    final LocalDateTime currentTime = LocalDateTime.now();
    return currentTime.minusDays(this.getSoftDeletedBatchRecordsRetentionDays());
  }
}
