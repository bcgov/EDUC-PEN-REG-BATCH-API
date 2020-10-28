package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.batch.schedulers.PenRegBatchScheduler;
import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class EventTaskScheduler {
  /**
   * The Task scheduler async service.
   */
  @Getter(PRIVATE)
  private final EventTaskSchedulerAsyncService taskSchedulerAsyncService;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param taskSchedulerAsyncService      the task scheduler async service
   */
  @Autowired
  public EventTaskScheduler(EventTaskSchedulerAsyncService taskSchedulerAsyncService) {
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
    getTaskSchedulerAsyncService().findAndProcessUncompletedSagas();
  }

  /**
   * Process repeats checked pen request batches.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.unprocessed.students.cron}")
  @SchedulerLock(name = "EXTRACT_UNPROCESSED_STUDENT_RECORDS",
      lockAtLeastFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.unprocessed.students.cron.lockAtMostFor}")
  @Transactional
  public void processLoadedPenRequestBatches() {
    LockAssert.assertLocked();
    getTaskSchedulerAsyncService().publishUnprocessedStudentRecords();
  }

  /**
   * Find all the pen request batch that has been processed and update their status and add history record.
   */
  @Scheduled(cron = "0 0/1 * * * *") // every 1 minutes
  @SchedulerLock(name = "MARK_PROCESSED_BATCHES_ACTIVE",
      lockAtLeastFor = "50s", lockAtMostFor = "52s")
  @Transactional
  public void markProcessedPenRequestBatchesActive() {
    LockAssert.assertLocked();
    getTaskSchedulerAsyncService().markProcessedBatchesActive();
  }

  /**
   * This is for edge case scenarios when the pod which was processing the batch file dies before persisting the repeat check updates.
   * please look at timing of this {@link PenRegBatchScheduler#extractUnProcessedFilesFromPenWebBlobs()}
   * as the timing of thi scheduler
   * Process all loaded pen request batches.
   */
  @Scheduled(cron = "0 0/12 * * * *") //every 12 minutes, it is dependent on the other scheduler timing.
  @SchedulerLock(name = "PROCESS_LOADED_BATCHES_FOR_REPEATS", lockAtLeastFor = "PT10M", lockAtMostFor = "PT11M")
  @Transactional
  public void processLoadedPenRequestBatchesForRepeats() {
    LockAssert.assertLocked();
    getTaskSchedulerAsyncService().processLoadedPenRequestBatchesForRepeats();
  }

  @Scheduled(cron = "0/1 * * * * *")
  @SchedulerLock(name = "EventTablePoller", lockAtLeastFor = "900ms", lockAtMostFor = "950ms")
  public void pollEventTableAndPublish() {
    LockAssert.assertLocked();
    getTaskSchedulerAsyncService().pollEventTableAndPublish();
  }

}
