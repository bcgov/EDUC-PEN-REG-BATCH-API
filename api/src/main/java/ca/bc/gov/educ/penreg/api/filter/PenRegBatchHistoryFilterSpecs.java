package ca.bc.gov.educ.penreg.api.filter;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

/**
 * The type Pen reg batch filter specs.
 */
@Service
@Slf4j
public class PenRegBatchHistoryFilterSpecs extends BaseFilterSpecs<PenRequestBatchHistoryEntity> {

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
  public PenRegBatchHistoryFilterSpecs(FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<PenRequestBatchHistoryEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<PenRequestBatchHistoryEntity, Integer> integerFilterSpecifications, FilterSpecifications<PenRequestBatchHistoryEntity, String> stringFilterSpecifications, FilterSpecifications<PenRequestBatchHistoryEntity, Long> longFilterSpecifications, FilterSpecifications<PenRequestBatchHistoryEntity, UUID> uuidFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }
}
