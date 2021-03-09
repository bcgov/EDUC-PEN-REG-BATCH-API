package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
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
public class PenRegBatchHistoryFilterSpecs {

  /**
   * The Date filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDate> dateFilterSpecifications;
  /**
   * The Date time filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  /**
   * The Integer filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, Integer> integerFilterSpecifications;
  /**
   * The String filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, String> stringFilterSpecifications;
  /**
   * The Long filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, Long> longFilterSpecifications;
  /**
   * The Uuid filter specifications.
   */
  private final FilterSpecifications<PenRequestBatchHistoryEntity, UUID> uuidFilterSpecifications;
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
  public PenRegBatchHistoryFilterSpecs(final FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDate> dateFilterSpecifications, final FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, final FilterSpecifications<PenRequestBatchHistoryEntity, Integer> integerFilterSpecifications, final FilterSpecifications<PenRequestBatchHistoryEntity, String> stringFilterSpecifications, final FilterSpecifications<PenRequestBatchHistoryEntity, Long> longFilterSpecifications, final FilterSpecifications<PenRequestBatchHistoryEntity, UUID> uuidFilterSpecifications, final Converters converters) {
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
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the date type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getDateTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(ChronoLocalDate.class), this.dateFilterSpecifications, associationNames);
  }

  /**
   * Gets date time type specification.
   *
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the date time type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getDateTimeTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(ChronoLocalDateTime.class), this.dateTimeFilterSpecifications, associationNames);
  }

  /**
   * Gets integer type specification.
   *
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the integer type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getIntegerTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(Integer.class), this.integerFilterSpecifications, associationNames);
  }

  /**
   * Gets long type specification.
   *
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the long type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getLongTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(Long.class), this.longFilterSpecifications, associationNames);
  }

  /**
   * Gets string type specification.
   *
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the string type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getStringTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(String.class), this.stringFilterSpecifications, associationNames);
  }

  /**
   * Gets uuid type specification.
   *
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param associationNames the association names
   * @return the uuid type specification
   */
  public Specification<PenRequestBatchHistoryEntity> getUUIDTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(UUID.class), this.uuidFilterSpecifications, associationNames);
  }

  /**
   * Gets specification.
   *
   * @param <T>              the type parameter
   * @param fieldName        the field name
   * @param filterValue      the filter value
   * @param filterOperation  the filter operation
   * @param converter        the converter
   * @param specifications   the specifications
   * @param associationNames the association names
   * @return the specification
   */
  private <T extends Comparable<T>> Specification<PenRequestBatchHistoryEntity> getSpecification(final String fieldName,
                                                                                          final String filterValue,
                                                                                          final FilterOperation filterOperation,
                                                                                          final Function<String, T> converter,
                                                                                          final FilterSpecifications<PenRequestBatchHistoryEntity, T> specifications,
                                                                                          final Associations associationNames) {
    final FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria, associationNames);
  }
}
