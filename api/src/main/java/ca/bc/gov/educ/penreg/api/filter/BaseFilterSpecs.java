package ca.bc.gov.educ.penreg.api.filter;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

/**
 * this is the generic class to support all kind of filter specifications for different entities
 *
 * @param <R> the entity type.
 * @author Om
 */
public abstract class BaseFilterSpecs<R> {

  private final FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications;
  private final FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final FilterSpecifications<R, Integer> integerFilterSpecifications;
  private final FilterSpecifications<R, String> stringFilterSpecifications;
  private final FilterSpecifications<R, Long> longFilterSpecifications;
  private final FilterSpecifications<R, UUID> uuidFilterSpecifications;
  private final Converters converters;

  /**
   * Instantiates a new Base filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  protected BaseFilterSpecs(FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<R, Integer> integerFilterSpecifications, FilterSpecifications<R, String> stringFilterSpecifications, FilterSpecifications<R, Long> longFilterSpecifications, FilterSpecifications<R, UUID> uuidFilterSpecifications, Converters converters) {
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
  public Specification<R> getDateTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  public Specification<R> getDateTimeTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  public Specification<R> getIntegerTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  public Specification<R> getLongTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  public Specification<R> getStringTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  public Specification<R> getUUIDTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation, final Associations associationNames) {
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
  private <T extends Comparable<T>> Specification<R> getSpecification(final String fieldName,
                                                                                          final String filterValue,
                                                                                          final FilterOperation filterOperation,
                                                                                          final Function<String, T> converter,
                                                                                          final FilterSpecifications<R, T> specifications,
                                                                                          final Associations associationNames) {
    final FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria, associationNames);
  }
}
