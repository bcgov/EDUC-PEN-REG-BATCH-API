package ca.bc.gov.educ.penreg.api.batch.schedulers;

import ca.bc.gov.educ.penreg.api.batch.input.TraxStudentWeb;
import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static lombok.AccessLevel.PRIVATE;


/**
 * The type Pen reg batch scheduler.
 */
@Component
@Slf4j
public class PenRegBatchScheduler {
  private final ExecutorService executorService = Executors.newFixedThreadPool(2);
  @Getter(PRIVATE)
  private final PenRegBatchProcessor penRegBatchProcessor;
  static final int MIN = 1000000;
  static final int MAX = 9999999;
  @Autowired
  public PenRegBatchScheduler(PenRegBatchProcessor penRegBatchProcessor) {
    this.penRegBatchProcessor = penRegBatchProcessor;
  }

  /**
   * Extract un processed files.
   * this method will only extract the file or blob, processing will be done in processor.
   */
  @Scheduled(fixedDelay = 600000, initialDelay = 600000) // every 10 minutes
  @SchedulerLock(name = "BatchFileExtractor",
      lockAtLeastFor = "590s", lockAtMostFor = "595s")
  public void extractUnProcessedFilesFromTSW() {
    log.info("extraction of batch file from TSW started.");
    try {
      executorService.execute(() -> {
        try {
          TraxStudentWeb tsw = generateDummyData(); // TODO this will be replaced by actual DB call.
          getPenRegBatchProcessor().processPenRegBatchFileFromTSW(tsw);
        } catch (IOException e) {
          log.error("Error reading file.", e);
        }
      });
    } catch (final Exception e) {
      log.error("Error reading file.", e);
    }

  }

  private TraxStudentWeb generateDummyData() throws IOException {
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample.txt")).getFile());
    byte[] bFile = Files.readAllBytes(file.toPath());
    var randomNum = (new Random().nextInt() * (MAX - MIN + 1) + MIN);
    return TraxStudentWeb.builder().extractDate(LocalDateTime.now()).fileName("demo").fileType("txt").fileContents(bFile).insertDate(LocalDateTime.now()).submissionNumber("T" + randomNum).build();
  }
}
