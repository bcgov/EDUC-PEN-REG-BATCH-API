package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PenCoordinatorService {
  private final RestUtils restUtils;

  @Autowired
  public PenCoordinatorService(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }


  public Optional<PenCoordinator> getPenCoordinatorByMinCode(final String mincode) {
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return Optional.empty();
    }

    return this.restUtils.getPenCoordinator(mincode);
  }

  public Optional<String> getPenCoordinatorEmailByMinCode(final String mincode) {
    log.debug("getting pen coordinator email for mincode :: {}", mincode);
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return Optional.empty();
    }
    return this.restUtils.getPenCoordinator(mincode).map(PenCoordinator::getPenCoordinatorEmail);
  }
}
