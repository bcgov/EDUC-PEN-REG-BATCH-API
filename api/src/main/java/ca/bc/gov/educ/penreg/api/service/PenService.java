package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen service.
 */
@Service
@Slf4j
public class PenService {


  /**
   * The Rest utils.
   */
  @Getter(PRIVATE)
  private final RestUtils restUtils;

  /**
   * Instantiates a new Pen service.
   *
   * @param restUtils      the rest utils
   */
  @Autowired
  public PenService(RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  /**
   * Gets next pen number.
   *
   * @param guid the guid to identify the transaction.
   * @return the next pen number
   */
  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public String getNextPenNumber(String guid) {
    return restUtils.getNextPenNumberFromPenServiceAPI(guid);
  }

}
