package ca.bc.gov.educ.penreg.api.batch.schedulers;

import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.util.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static lombok.AccessLevel.PRIVATE;


/**
 * The type Pen reg batch scheduler.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class PenRegBatchScheduler implements Closeable {
  /**
   * The constant FILE_TYPE_PEN.
   */
  public static final String FILE_TYPE_PEN = "PEN";
  /**
   * The Executor service.
   */
  private final ExecutorService executorService;
  /**
   * The Pen reg batch processor.
   */
  @Getter(PRIVATE)
  private final PenRegBatchProcessor penRegBatchProcessor;
  /**
   * The Pen request batch file service.
   */
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
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().withNameFormat("pen-web-blob-processor-%d").get();
    this.executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
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
    LockAssert.assertLocked();
    var unExtractedRecords = getPenRequestBatchFileService().getAllNotExtractedRecords(FILE_TYPE_PEN); // PEN is the file type based on which records will be filtered.
    if (!unExtractedRecords.isEmpty()) {
      log.info("{} :: records found where extract date is null", unExtractedRecords.size());
      for (var penWebBlob : unExtractedRecords) {
        executorService.execute(() -> getPenRegBatchProcessor().processPenRegBatchFileFromPenWebBlob(penWebBlob));
      }
    } else {
      log.debug("No Records found to be processed.");
    }
  }

  /**
   * Close.
   */
  @Override
  public void close() {
    if (!executorService.isShutdown()) {
      log.info("shutting down executorService in PenRegBatchScheduler");
      executorService.shutdown();
    }
  }
}
