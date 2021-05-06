package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.constants.MatchAlgorithmStatusCode;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestPenMatchProcessingPayload;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("penRequestPenMatchResultProcessingService")
public class PenRequestPenMatchResultProcessingService extends BasePenMatchResultProcessingService<PenRequestPenMatchProcessingPayload, Pair<Integer, Optional<PenRequestResult>>> {

  /**
   * The constant studentMapper.
   */

  public PenRequestPenMatchResultProcessingService(final RestUtils restUtils, final PenService penService) {
    super(restUtils, penService);
  }


  @Override
  public Pair<Integer, Optional<PenRequestResult>> processPenMatchResults(final PenRequestPenMatchProcessingPayload payload) {
    val penMatchResult = payload.getPenMatchResult();
    if (penMatchResult == null || penMatchResult.getPenStatus() == null) {
      log.error("Pen match result invalid :: {}", penMatchResult == null ? "null" : penMatchResult);
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    final var algorithmStatusCode = MatchAlgorithmStatusCode.valueOf(penMatchResult.getPenStatus());
    return super.handleBasedOnPenStatus(algorithmStatusCode, payload);
  }


  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleDefault(final PenRequestPenMatchProcessingPayload payload) {
    return Pair.of(HttpStatus.MULTIPLE_CHOICES.value(), Optional.empty());
  }

  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleF1Status(final PenRequestPenMatchProcessingPayload payload) {
    return this.handleDefault(payload);
  }

  @SneakyThrows(JsonProcessingException.class)
  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleCreateNewStudentStatus(final PenRequestPenMatchProcessingPayload payload) {
    val penRequestResult = payload.getPenRequestResult();
    val pen = super.generateNewPen(UUID.randomUUID().toString());
    val student = PenRequestBatchMapper.mapper.toStudent(payload.getPenRequest(), pen);
    val createStudentEvent = Event.builder().sagaId(payload.getTransactionID()).eventType(EventType.CREATE_STUDENT).eventPayload(JsonUtil.getJsonStringFromObject(student)).build();
    val createdStudent = this.getRestUtils().requestEventResponseFromStudentAPI(createStudentEvent);
    if (createdStudent.isPresent() && createdStudent.get().getEventOutcome() == EventOutcome.STUDENT_CREATED) {
      penRequestResult.setPen(pen);
      return Pair.of(HttpStatus.CREATED.value(), Optional.of(penRequestResult));
    }
    return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
  }

  @Override
  protected Pair<Integer, Optional<PenRequestResult>> handleSystemMatchedStatus(final PenRequestPenMatchProcessingPayload payload) {
    if (CollectionUtils.isEmpty(payload.getPenMatchResult().getMatchingRecords())) {
      log.error("Matching record should not be blank for sys matched status");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val firstStudent = payload.getPenMatchResult().getMatchingRecords().stream().findFirst();

    if (firstStudent.isEmpty() || StringUtils.isBlank(firstStudent.get().getMatchingPEN())) {
      log.error("Pen match result does not contain matched student for sys match status ");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val optionalStudent = this.getRestUtils().getStudentByPEN(firstStudent.get().getMatchingPEN());
    if (optionalStudent.isEmpty()) {
      log.error("could not retrieve student data for sys match status ");
      return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), Optional.empty());
    }
    val penRequestResult = PenRequestBatchMapper.mapper.toPenRequestResult(optionalStudent.get());
    return Pair.of(HttpStatus.OK.value(), Optional.of(penRequestResult));
  }
}
