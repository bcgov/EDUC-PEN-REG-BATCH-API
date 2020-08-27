package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * The type Pen reg batch filter specs.
 */
@Service
@Slf4j
public class PenRegBatchFilterSpecs {

  /**
   * The Date filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, ChronoLocalDate> dateFilterSpecifications;
  /**
   * The Date time filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  /**
   * The Integer filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, Integer> integerFilterSpecifications;
  /**
   * The String filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, String> stringFilterSpecifications;
  /**
   * The Long filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, Long> longFilterSpecifications;
  /**
   * The Uuid filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchEntity, UUID> uuidFilterSpecifications;
  /**
   * The Converters.
   */
  private final Converters converters;

  /**
   * Instantiates a new Pen reg batch filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  public PenRegBatchFilterSpecs(FilterSpecifications<PenRequestBatchEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Integer> integerFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, String> stringFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Long> longFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, UUID> uuidFilterSpecifications, Converters converters) {
    this.dateFilterSpecifications = dateFilterSpecifications;
    this.dateTimeFilterSpecifications = dateTimeFilterSpecifications;
    this.integerFilterSpecifications = integerFilterSpecifications;
    this.stringFilterSpecifications = stringFilterSpecifications;
    this.longFilterSpecifications = longFilterSpecifications;
    this.uuidFilterSpecifications = uuidFilterSpecifications;
    this.converters = converters;
  }

  /**
   * Gets date type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date type specification
   */
  public Specification<PenRequestBatchEntity> getDateTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDate.class), dateFilterSpecifications);
  }

  /**
   * Gets date time type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date time type specification
   */
  public Specification<PenRequestBatchEntity> getDateTimeTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDateTime.class), dateTimeFilterSpecifications);
  }

  /**
   * Gets integer type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the integer type specification
   */
  public Specification<PenRequestBatchEntity> getIntegerTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Integer.class), integerFilterSpecifications);
  }

  /**
   * Gets long type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the long type specification
   */
  public Specification<PenRequestBatchEntity> getLongTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Long.class), longFilterSpecifications);
  }

  /**
   * Gets string type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the string type specification
   */
  public Specification<PenRequestBatchEntity> getStringTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(String.class), stringFilterSpecifications);
  }

  /**
   * Gets uuid type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the uuid type specification
   */
  public Specification<PenRequestBatchEntity> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(UUID.class), uuidFilterSpecifications);
  }

  /**
   * Gets specification.
   *
   * @param <T>             the type parameter
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @param converter       the converter
   * @param specifications  the specifications
   * @return the specification
   */
  private <T extends Comparable<T>> Specification<PenRequestBatchEntity> getSpecification(String fieldName,
                                                                                          String filterValue,
                                                                                          FilterOperation filterOperation,
                                                                                          Function<String, T> converter,
                                                                                          FilterSpecifications<PenRequestBatchEntity, T> specifications) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
