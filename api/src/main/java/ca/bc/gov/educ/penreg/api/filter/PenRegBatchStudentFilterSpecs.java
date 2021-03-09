package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
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
public class PenRegBatchStudentFilterSpecs extends BaseFilterSpecs<PenRequestBatchStudentEntity> {

  /**
   * Instantiates a new Student filter specs.
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
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }
}
