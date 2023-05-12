package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The type Pen reg batch filter specs.
 */
@Service
@Slf4j
public class PenRegBatchFilterSpecs extends BaseFilterSpecs<PenRequestBatchEntity> {

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
  public PenRegBatchFilterSpecs(FilterSpecifications<PenRequestBatchEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Integer> integerFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, String> stringFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, Long> longFilterSpecifications, FilterSpecifications<PenRequestBatchEntity, UUID> uuidFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }

}
