package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SagaFilterSpecs extends BaseFilterSpecs<Saga>{

  /**
   * Instantiates a new saga filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  public SagaFilterSpecs(FilterSpecifications<Saga, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<Saga, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<Saga, Integer> integerFilterSpecifications, FilterSpecifications<Saga, String> stringFilterSpecifications, FilterSpecifications<Saga, Long> longFilterSpecifications, FilterSpecifications<Saga, UUID> uuidFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }
}
