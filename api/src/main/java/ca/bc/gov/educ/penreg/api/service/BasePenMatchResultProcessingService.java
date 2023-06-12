package ca.bc.gov.educ.penreg.api.service;

import static lombok.AccessLevel.PROTECTED;

import ca.bc.gov.educ.penreg.api.constants.MatchAlgorithmStatusCode;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.interfaces.PenMatchResultProcessingService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestValidationIssue;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePenMatchResultProcessingService<T, R> implements PenMatchResultProcessingService<T, R> {
  @Getter(PROTECTED)
  private final RestUtils restUtils;
  /**
   * The Pen service.
   */
  @Getter(PROTECTED)
  private final PenService penService;

  protected BasePenMatchResultProcessingService(final RestUtils restUtils, final PenService penService) {
    this.restUtils = restUtils;
    this.penService = penService;
  }

  /**
   * Generate new pen string.
   *
   * @param guid the guid to identify the transaction.
   * @return the string
   */
  protected String generateNewPen(final String guid) {
    log.info("generate new pen called for guid :: {}", guid);
    final String pen = this.getPenService().getNextPenNumber(guid);
    log.info("got new pen :: {} for guid :: {}", pen, guid);
    return pen;
  }


  protected R handleBasedOnPenStatus(final MatchAlgorithmStatusCode algorithmStatusCode, final T t) {
    final R r;
    switch (algorithmStatusCode) {
      case AA:
      case B1:
      case C1:
      case D1:
        r = this.handleSystemMatchedStatus(t);
        break;
      case B0:
      case C0:
      case D0:
        r = this.handleCreateNewStudentStatus(t);
        break;
      case F1:
        r = this.handleF1Status(t); // FIXABLE
        break;
      case BM:
      case CM:
      case DM:
      case G0:
      default:
        r = this.handleDefault(t);
        break;
    }
    return r;
  }
  /**
   * check if there is a warning present in the validation.
   *
   * @param validationIssueEntities the validation issues
   * @return the boolean true if validation of grade code resulted in warning else false.
   */
  protected boolean isGradeCodeWarningPresent(final List<PenRequestValidationIssue> validationIssueEntities) {
    return validationIssueEntities.stream().anyMatch(entity -> "GRADECODE".equals(entity.getPenRequestBatchValidationFieldCode()));
  }

  /**
   * check if there is a warning present in the validation.
   *
   * @param validationIssueEntities the validation issues
   * @return the boolean true if validation of grade code resulted in warning else false.
   */
  protected boolean isGradeCodeWarningPresentForEntity(final Set<PenRequestBatchStudentValidationIssueEntity> validationIssueEntities) {
    return validationIssueEntities.stream().anyMatch(entity -> "GRADECODE".equals(entity.getPenRequestBatchValidationFieldCode()));
  }

  protected abstract R handleDefault(T t);

  protected abstract R handleF1Status(T t);

  protected abstract R handleCreateNewStudentStatus(T t);

  protected abstract R handleSystemMatchedStatus(T t);
}
