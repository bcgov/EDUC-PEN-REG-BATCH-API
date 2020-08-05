package ca.bc.gov.educ.penreg.api.batch.schedulers;

import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static lombok.AccessLevel.PRIVATE;


/**
 * The type Pen reg batch scheduler.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class PenRegBatchScheduler implements Closeable {
  private final ExecutorService executorService = Executors.newFixedThreadPool(5);
  @Getter(PRIVATE)
  private final PenRegBatchProcessor penRegBatchProcessor;
  @Getter(PRIVATE)
  private final PenRequestBatchFileService penRequestBatchFileService;

  /**
   * Instantiates a new Pen reg batch scheduler.
   *
   * @param penRegBatchProcessor       the pen reg batch processor
   * @param penRequestBatchFileService the pen request batch file service
   */
  @Autowired
  public PenRegBatchScheduler(PenRegBatchProcessor penRegBatchProcessor, PenRequestBatchFileService penRequestBatchFileService) {
    this.penRegBatchProcessor = penRegBatchProcessor;
    this.penRequestBatchFileService = penRequestBatchFileService;
  }

  /**
   * Extract un processed files.
   * this method will only extract the file or blob, processing will be done in processor.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.unprocessed.pen.web.blobs.cron}")
  @SchedulerLock(name = "EXTRACT_UNPROCESSED_PEN_WEB_BLOB",
      lockAtLeastFor = "${scheduled.jobs.extract.unprocessed.pen.web.blobs.cron.lockAtLeastFor}",
      lockAtMostFor = "${scheduled.jobs.extract.unprocessed.pen.web.blobs.cron.lockAtMostFor}")
  public void extractUnProcessedFilesFromPenWebBlobs() {
    log.info("extraction of batch file from Pen Web Blobs started.");
    var unExtractedRecords = getPenRequestBatchFileService().getAllNotExtractedRecords("PEN"); // PEN is the file type based on which records will be filtered.
    if (!unExtractedRecords.isEmpty()) {
      log.info("{} :: records found where extract date is null", unExtractedRecords.size());
      List<Future<String>> futureResults = new ArrayList<>(unExtractedRecords.size());
      for (var penWebBlob : unExtractedRecords) {
        final Callable<String> callable = () -> getPenRegBatchProcessor().processPenRegBatchFileFromPenWebBlob(penWebBlob);
        futureResults.add(executorService.submit(callable));
      }
      // this will make sure that this thread waits for all the processing threads to complete.
      for (final Future<String> futureResult : futureResults) {
        try {
          futureResult.get(); //wait for all the threads to complete
        } catch (ExecutionException | InterruptedException e) {
          log.warn("Error waiting for result", e);
        }
      }
    }else {
      log.info("No Records found to be processed.");
    }
  }

  @Override
  public void close() {
    if (!executorService.isShutdown()) {
      log.info("shutting down executorService in PenRegBatchScheduler");
      executorService.shutdown();
    }
  }
}
