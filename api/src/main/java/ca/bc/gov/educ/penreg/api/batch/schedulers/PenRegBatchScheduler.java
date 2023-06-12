package ca.bc.gov.educ.penreg.api.batch.schedulers;

import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * The type Pen reg batch scheduler.
 */
@Component
@Slf4j
public class PenRegBatchScheduler {
  /**
   * The constant FILE_TYPE_PEN.
   */
  public static final String FILE_TYPE_PEN = "PEN";
  private final StringRedisTemplate stringRedisTemplate;
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
  public PenRegBatchScheduler(final PenRegBatchProcessor penRegBatchProcessor,
                              final PenRequestBatchFileService penRequestBatchFileService, final RedisConnectionFactory redisConnectionFactory) {
    this.penRegBatchProcessor = penRegBatchProcessor;
    this.penRequestBatchFileService = penRequestBatchFileService;
    this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
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
    log.debug("Launching nighttime batch extract job");
    final var unExtractedRecords = this.getPenRequestBatchFileService().getAllNotExtractedRecords(FILE_TYPE_PEN); // PEN is the file type based on which records will be filtered.
    runBatchLoad(unExtractedRecords);
  }

  /**
   * Extract un processed files.
   * this method will only extract the file or blob, processing will be done in processor.
   */
  @Scheduled(cron = "${scheduled.jobs.extract.unprocessed.penwebfiles.pen.web.blobs.cron}")
  @SchedulerLock(name = "EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOB",
    lockAtLeastFor = "${scheduled.jobs.extract.unprocessed.pen.web.blobs.cron.lockAtLeastFor}",
    lockAtMostFor = "${scheduled.jobs.extract.unprocessed.pen.web.blobs.cron.lockAtMostFor}")
  public void extractUnProcessedPenWebFilesFromPenWebBlobs() {
    LockAssert.assertLocked();
    log.debug("Launching daytime PENWEB batch extract job");
    final var unExtractedRecords = this.getPenRequestBatchFileService().getAllPenWebNotExtractedRecords(FILE_TYPE_PEN); // PEN is the file type based on which records will be filtered.
    runBatchLoad(unExtractedRecords);
  }

  private void runBatchLoad(List<PENWebBlobEntity> unExtractedRecords){
    if (!unExtractedRecords.isEmpty()) {
      log.info("{} :: records found where extract date is null", unExtractedRecords.size());
      for (final var penWebBlob : unExtractedRecords) {
        final String redisKey = penWebBlob.getSubmissionNumber().concat(
          "::extractUnProcessedFilesFromPenWebBlobs");
        val valueFromRedis = this.stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isBlank(valueFromRedis)) { // skip if it is already in redis
          // put it in redis for 5 minutes, it is expected the file processing wont take more than that and if the
          // pod dies in between the scheduler will pick it up and process it again after the lock is released(5
          // minutes).
          this.stringRedisTemplate.opsForValue().set(redisKey, "true", 5, TimeUnit.MINUTES);
          this.getPenRegBatchProcessor().processPenRegBatchFileFromPenWebBlob(penWebBlob);
        } else {
          log.debug("skipping {} record, as it is already processed or being processed.", redisKey);
        }

      }
    } else {
      log.debug("No Records found to be processed.");
    }
  }

}
