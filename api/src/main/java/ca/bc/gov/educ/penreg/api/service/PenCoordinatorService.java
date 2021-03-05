package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.model.v1.Mincode;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.repository.PenCoordinatorRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PenCoordinatorService {
  private final PenCoordinatorRepository penCoordinatorRepository;
  private final ReadWriteLock penCoordinatorMapLock = new ReentrantReadWriteLock();
  private Map<Mincode, PenCoordinator> penCoordinatorMap;

  @Autowired
  public PenCoordinatorService(final PenCoordinatorRepository penCoordinatorRepository) {
    this.penCoordinatorRepository = penCoordinatorRepository;
  }

  @PostConstruct
  public void init() {
    this.penCoordinatorMap = this.penCoordinatorRepository.findAll().stream().map(PenCoordinatorMapper.mapper::toTrimmedPenCoordinator).collect(Collectors.toConcurrentMap(PenCoordinator::getMincode, Function.identity()));
  }

  @Scheduled(cron = "0 0 0/4 * * *") // every 4 hours
  public void scheduled() {
    final Lock writeLock = this.penCoordinatorMapLock.writeLock();
    try {
      writeLock.lock();
      this.init();
    } finally {
      writeLock.unlock();
    }
  }

  public Optional<PenCoordinator> getPenCoordinatorByMinCode(final Mincode mincode) {
    return Optional.ofNullable(this.penCoordinatorMap.get(mincode));
  }

  public Optional<PenCoordinator> getPenCoordinatorByMinCode(final String mincode) {
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return Optional.empty();
    }
    val ministryCode = Mincode.builder().districtNumber(Integer.parseInt(mincode.substring(0, 3))).schoolNumber(Integer.parseInt(mincode.substring(3, 8))).build();
    return Optional.ofNullable(this.penCoordinatorMap.get(ministryCode));
  }

  public Optional<String> getPenCoordinatorEmailByMinCode(final String mincode) {
    log.debug("getting pen coordinator email for mincode :: {}", mincode);
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return Optional.empty();
    }
    val ministryCode = Mincode.builder().districtNumber(Integer.parseInt(mincode.substring(0, 3))).schoolNumber(Integer.parseInt(mincode.substring(3, 8))).build();
    val penCoordinator = this.penCoordinatorMap.get(ministryCode);
    if (penCoordinator != null) {
      log.debug("pen coordinator email for mincode :: {} is :: {} ", mincode, penCoordinator.getPenCoordinatorEmail());
      return Optional.of(penCoordinator.getPenCoordinatorEmail());
    }
    return Optional.empty();
  }
}
