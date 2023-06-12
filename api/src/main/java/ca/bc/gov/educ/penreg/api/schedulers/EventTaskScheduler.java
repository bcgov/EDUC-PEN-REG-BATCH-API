package ca.bc.gov.educ.penreg.api.schedulers;

import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
public class EventTaskScheduler {
  /**
   * The Task scheduler async service.
   */
  @Getter(PRIVATE)
  private final EventTaskSchedulerAsyncService taskSchedulerAsyncService;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param taskSchedulerAsyncService the task scheduler async service
   */
  @Autowired
  public EventTaskScheduler(final EventTaskSchedulerAsyncService taskSchedulerAsyncService) {
    this.taskSchedulerAsyncService = taskSchedulerAsyncService;
  }


  /**
   * Run the job every minute to check how many records are in IN_PROGRESS or STARTED status and has not been updated in last 5 minutes.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.uncompleted.sagas.cron}") // 1 * * * * *
  @SchedulerLock(name = "EXTRACT_UNCOMPLETED_SAGAS",
      lockAtLeastFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtMostFor}")
  @Transactional
  public void findAndProcessPendingSagaEvents() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAndProcessUncompletedSagas();
  }

  /**
   * Process repeats checked pen request batches.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.unprocessed.students.cron}")
  @SchedulerLock(name = "EXTRACT_UNPROCESSED_STUDENT_RECORDS",
      lockAtLeastFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtMostFor}")
  @Transactional
  public void publishRepeatCheckedStudentsForFurtherProcessing() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().publishRepeatCheckedStudentsForFurtherProcessing();
  }

  /**
   * Find all the pen request batch that has been processed and update their status and add history record.
   */
  @Scheduled(cron = "${scheduled.jobs.mark.processed.batches.active.cron}") // every 1 minutes "0 0/1 * * * *"
  @SchedulerLock(name = "MARK_PROCESSED_BATCHES_ACTIVE",
      lockAtLeastFor = "${scheduled.jobs.mark.processed.batches.active.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.mark.processed.batches.active.cron.lockAtMostFor}")
  @Transactional
  public void markProcessedPenRequestBatchesActiveOrArchived() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().markProcessedBatchesActiveOrArchived();
  }

  /**
   * This is EITHER for the edge case scenarios when the pod which was processing the batch file dies before persisting
   * the repeat check updates.
   * OR when the file was held for certain condition and it was released by pen coordinator for further processing.
   */
  @Scheduled(cron = "${scheduled.jobs.process.loaded.batches.for.repeats.cron}") // every 1 minutes "0 0/1 * * * *"
  @SchedulerLock(name = "PROCESS_LOADED_BATCHES_FOR_REPEATS", lockAtLeastFor = "${scheduled.jobs.process.loaded.batches.for.repeats.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.loaded.batches.for.repeats.cron.lockAtMostFor}")
  @Transactional
  public void processLoadedPenRequestBatchesForRepeats() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().checkLoadedStudentRecordsForDuplicatesAndRepeatsAndPublishForFurtherProcessing();
  }


}
