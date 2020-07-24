package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class PenRegBatchFilterSpecs {

  private final FilterSpecifications<PenRequestBatchEntity, ChronoLocalDate> dateFilterSpecifications;
  private final FilterSpecifications<PenRequestBatchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final FilterSpecifications<PenRequestBatchEntity, Integer> integerFilterSpecifications;
  private final FilterSpecifications<PenRequestBatchEntity, String> stringFilterSpecifications;
  private final FilterSpecifications<PenRequestBatchEntity, Long> longFilterSpecifications;
  private final FilterSpecifications<PenRequestBatchEntity, UUID> uuidFilterSpecifications;
  private final Converters converters;

  public PenRegBatchFilterSpecs(FilterSpecifications<PenRequestBatchEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Integer> integerFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, String> stringFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Long> longFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, UUID> uuidFilterSpecifications, Converters converters) {
    this.dateFilterSpecifications = dateFilterSpecifications;
    this.dateTimeFilterSpecifications = dateTimeFilterSpecifications;
    this.integerFilterSpecifications = integerFilterSpecifications;
    this.stringFilterSpecifications = stringFilterSpecifications;
    this.longFilterSpecifications = longFilterSpecifications;
    this.uuidFilterSpecifications = uuidFilterSpecifications;
    this.converters = converters;
  }

  public Specification<PenRequestBatchEntity> getDateTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDate.class), dateFilterSpecifications);
  }

  public Specification<PenRequestBatchEntity> getDateTimeTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDateTime.class), dateTimeFilterSpecifications);
  }

  public Specification<PenRequestBatchEntity> getIntegerTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Integer.class), integerFilterSpecifications);
  }

  public Specification<PenRequestBatchEntity> getLongTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Long.class), longFilterSpecifications);
  }

  public Specification<PenRequestBatchEntity> getStringTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(String.class), stringFilterSpecifications);
  }
  public Specification<PenRequestBatchEntity> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(UUID.class), uuidFilterSpecifications);
  }

  private <T extends Comparable<T>> Specification<PenRequestBatchEntity> getSpecification(String fieldName,
                                                                                          String filterValue,
                                                                                          FilterOperation filterOperation,
                                                                                          Function<String, T> converter,
                                                                                          FilterSpecifications<PenRequestBatchEntity, T> specifications) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
