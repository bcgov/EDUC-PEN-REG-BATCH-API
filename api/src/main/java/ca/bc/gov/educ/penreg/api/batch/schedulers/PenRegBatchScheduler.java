package ca.bc.gov.educ.penreg.api.batch.schedulers;

import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;


/**
 * The type Pen reg batch scheduler.
 */
@Component
@Slf4j
public class PenRegBatchScheduler {

  @Getter(PRIVATE)
  private final PenRegBatchProcessor penRegBatchProcessor;

  @Autowired
  public PenRegBatchScheduler(PenRegBatchProcessor penRegBatchProcessor) {
    this.penRegBatchProcessor = penRegBatchProcessor;
  }

  /**
   * Extract un processed files.
   * this method will only extract the file or blob, processing will be done in processor.
   */
  @Scheduled(fixedDelay = 600000) // every 10 minutes
  @SchedulerLock(name = "BatchFileExtractor",
      lockAtLeastFor = "590s", lockAtMostFor = "595s")
  public void extractUnProcessedFilesFromTSW(){
    log.info("extraction of batch file from TSW started.");
    getPenRegBatchProcessor().processPenRegBatchFileFromTSW();
  }
}
