package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * this class is responsible to mark the file duplicate based on below.
 * <pre>
 *   When a PSI batch file (a batch file with mincode 102) is submitted and the submission is a duplicate (exactly the same) as a previous submission
 *   for the PSI that occurred in the last 48 hours, the file should be held for user review:
 *   Note the only difference in the file will be the submission number
 * </pre>
 */
@Service
@Slf4j
public class PSIDuplicateFileCheckService implements DuplicateFileCheckService {
  private final PenWebBlobRepository penWebBlobRepository;


  public PSIDuplicateFileCheckService(final PenWebBlobRepository penWebBlobRepository) {
    this.penWebBlobRepository = penWebBlobRepository;
  }

  /**
   * find all the files that does not have the same submission number and determine if the contents are same.
   *
   * @param penWebBlobEntity the entity to process for duplication check.
   * @return true or false o
   */
  @Override
  public boolean isBatchFileDuplicate(final PENWebBlobEntity penWebBlobEntity) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LocalDateTime dateTimeToCompare = LocalDateTime.now().minusHours(48);
    val penWebBlobs = this.penWebBlobRepository.findAllByMincodeAndInsertDateTimeGreaterThanAndSubmissionNumberNotAndFileTypeAndExtractDateTimeIsNotNull(penWebBlobEntity.getMincode(), dateTimeToCompare, penWebBlobEntity.getSubmissionNumber(), "PEN");
    final boolean result = penWebBlobs.stream().anyMatch(element -> Arrays.equals(element.getFileContents(), penWebBlobEntity.getFileContents()));
    stopwatch.stop();
    log.info("Time taken for file duplicate check is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  @Override
  public SchoolGroupCodes getSchoolGroupCode() {
    return SchoolGroupCodes.PSI;
  }

}
