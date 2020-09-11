package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
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
public class PenRegBatchStudentFilterSpecs {

  /**
   * The Date filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, ChronoLocalDate> dateFilterSpecifications;
  /**
   * The Date time filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  /**
   * The Integer filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, Integer> integerFilterSpecifications;
  /**
   * The String filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, String> stringFilterSpecifications;
  /**
   * The Long filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, Long> longFilterSpecifications;
  /**
   * The Uuid filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchStudentEntity, UUID> uuidFilterSpecifications;
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
  public PenRegBatchStudentFilterSpecs(FilterSpecifications<PenRequestBatchStudentEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<PenRequestBatchStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<PenRequestBatchStudentEntity, Integer> integerFilterSpecifications, FilterSpecifications<PenRequestBatchStudentEntity, String> stringFilterSpecifications, FilterSpecifications<PenRequestBatchStudentEntity, Long> longFilterSpecifications, FilterSpecifications<PenRequestBatchStudentEntity, UUID> uuidFilterSpecifications, Converters converters) {
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
  public Specification<PenRequestBatchStudentEntity> getDateTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  public Specification<PenRequestBatchStudentEntity> getDateTimeTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  public Specification<PenRequestBatchStudentEntity> getIntegerTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  public Specification<PenRequestBatchStudentEntity> getLongTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  public Specification<PenRequestBatchStudentEntity> getStringTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  public Specification<PenRequestBatchStudentEntity> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
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
  private <T extends Comparable<T>> Specification<PenRequestBatchStudentEntity> getSpecification(String fieldName,
                                                                                          String filterValue,
                                                                                          FilterOperation filterOperation,
                                                                                          Function<String, T> converter,
                                                                                          FilterSpecifications<PenRequestBatchStudentEntity, T> specifications) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
