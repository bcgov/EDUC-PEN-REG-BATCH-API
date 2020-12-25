package ca.bc.gov.educ.penreg.api.schedulers;

import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class PurgeOldSagaRecordsScheduler {
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SagaEventRepository sagaEventRepository;

  @Value("${purge.records.saga.after.days}")
  @Setter
  @Getter
  Integer sagaRecordStaleInDays;

  public PurgeOldSagaRecordsScheduler(SagaRepository sagaRepository, SagaEventRepository sagaEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }


  /**
   * run the job based on configured scheduler(a cron expression) and purge old records from DB.
   */
  @Scheduled(cron = "${scheduled.jobs.purge.old.saga.records.cron}")
  @SchedulerLock(name = "PurgeOldSagaRecordsLock",
      lockAtLeastFor = "55s", lockAtMostFor = "57s")
  @Transactional
  public void pollSagaTableAndPurgeOldRecords() {
    LockAssert.assertLocked();
    LocalDateTime createDateToCompare = calculateCreateDateBasedOnStaleSagaRecordInDays();
    List<SagaEvent> sagaEventList = new CopyOnWriteArrayList<>();
    var sagas = getSagaRepository().findAllByCreateDateBefore(createDateToCompare);
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        sagaEventList.addAll(getSagaEventRepository().findBySaga(saga));
      }
    }
    sagaEventRepository.deleteAll(sagaEventList);
    sagaRepository.deleteAll(sagas);
  }

  private LocalDateTime calculateCreateDateBasedOnStaleSagaRecordInDays() {
    LocalDateTime currentTime = LocalDateTime.now();
    return currentTime.minusDays(getSagaRecordStaleInDays());
  }
}
