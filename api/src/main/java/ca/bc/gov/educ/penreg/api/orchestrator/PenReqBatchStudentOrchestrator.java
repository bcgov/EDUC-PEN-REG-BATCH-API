package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.PenMatchSagaMapper;
import ca.bc.gov.educ.penreg.api.mappers.PenStudentDemogValidationMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentOrchestratorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen req batch student orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchStudentOrchestrator extends BaseOrchestrator<PenRequestBatchStudentSagaData> {

  /**
   * The constant penMatchSagaMapper.
   */
  private static final PenMatchSagaMapper penMatchSagaMapper = PenMatchSagaMapper.mapper;
  /**
   * The constant validationMapper.
   */
  private static final PenStudentDemogValidationMapper validationMapper = PenStudentDemogValidationMapper.mapper;
  /**
   * The Pen request batch student orchestrator service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentOrchestratorService penRequestBatchStudentOrchestratorService;


  /**
   * Instantiates a new Pen req batch student orchestrator.
   *
   * @param sagaService                               the saga service
   * @param messagePublisher                          the message publisher
   * @param penRequestBatchStudentOrchestratorService the pen request batch student orchestrator service
   */
  @Autowired
  public PenReqBatchStudentOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                        PenRequestBatchStudentOrchestratorService penRequestBatchStudentOrchestratorService) {
    super(sagaService, messagePublisher, PenRequestBatchStudentSagaData.class, PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString());
    this.penRequestBatchStudentOrchestratorService = penRequestBatchStudentOrchestratorService;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .step(READ_FROM_TOPIC, READ_FROM_TOPIC_SUCCESS, VALIDATE_STUDENT_DEMOGRAPHICS, this::validateStudentDemographics)
        .step(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_NO_ERROR_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
        .step(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_WITH_ONLY_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
        .step(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_WITH_ERROR, MARK_SAGA_COMPLETE, this::markSagaComplete)
        .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
        .step(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }


  /**
   * Validate student demographics.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  private void validateStudentDemographics(Event event, Saga saga, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    var validationPayload = getPenRequestBatchStudentOrchestratorService()
        .scrubValidationPayload(validationMapper.toStudentDemogValidationPayload(penRequestBatchStudentSagaData));

    validationPayload.setTransactionID(saga.getSagaId().toString());
    var eventPayload = JsonUtil.getJsonString(validationPayload);
    if (eventPayload.isPresent()) {
      Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
          .replyTo(getTopicToSubscribe())
          .eventPayload(eventPayload.get())
          .build();
      postMessageToTopic(PEN_VALIDATION_API_TOPIC.toString(), nextEvent);
      log.info("message sent to PEN_VALIDATION_API_TOPIC for VALIDATE_STUDENT_DEMOGRAPHICS Event. :: {}", saga.getSagaId());
    } else {
      log.error("event payload is not present this should not have happened. :: {}", saga.getSagaId());
    }
  }


  /**
   * it will hand off the request to downstream service class to process the results.
   * please see
   * {@link PenRequestBatchStudentOrchestratorService#processPenMatchResult(Saga, PenRequestBatchStudentSagaData, PenMatchResult)}
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  private void processPenMatchResults(Event event, Saga saga, PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH_RESULTS.toString());
    var penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, event.getEventPayload());
    penRequestBatchStudentSagaData.setPenMatchResult(penMatchResult); // update the original payload with response from PEN_MATCH_API
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData)); // save the updated payload to DB...
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    if (!penMatchResult.getMatchingRecords().isEmpty()) {
      getPenRequestBatchStudentOrchestratorService().persistPossibleMatches(saga.getPenRequestBatchStudentID(), penMatchResult.getMatchingRecords());
    }
    var eventOptional = getPenRequestBatchStudentOrchestratorService().processPenMatchResult(saga, penRequestBatchStudentSagaData, penMatchResult);
    if (eventOptional.isPresent()) {
      executeSagaEvent(eventOptional.get());
    } else {
      executeSagaEvent(Event.builder().sagaId(saga.getSagaId())
          .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
          .build());
    }

  }


  /**
   * Process pen match.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  private void processPenMatch(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws JsonProcessingException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    // need to persist the validation warnings from response payload.
    saveDemogValidationResults(event, penRequestBatchStudentSagaData);
    var eventPayload = JsonUtil.getJsonString(penMatchSagaMapper.toPenMatchStudent(penRequestBatchStudentSagaData));
    if (eventPayload.isPresent()) {
      Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(PROCESS_PEN_MATCH)
          .replyTo(getTopicToSubscribe())
          .eventPayload(eventPayload.get())
          .build();
      postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
      log.info("message sent to PEN_MATCH_API_TOPIC for PROCESS_PEN_MATCH Event. :: {}", saga.getSagaId());
    } else {
      log.error("event payload is not present this should not have happened. :: {}", saga.getSagaId());
    }

  }

  /**
   * Save demog validation results.
   *
   * @param event    the event
   * @param sagaData the saga data
   */
  @Override
  protected void saveDemogValidationResults(Event event, PenRequestBatchStudentSagaData sagaData) {
    if (event.getEventType() == VALIDATE_STUDENT_DEMOGRAPHICS
        && StringUtils.isNotBlank(event.getEventPayload())) {
      PenRequestBatchStudentStatusCodes statusCode;
      if (event.getEventOutcome() == VALIDATION_SUCCESS_WITH_ERROR) {
        statusCode = PenRequestBatchStudentStatusCodes.ERROR;
      } else {
        statusCode = PenRequestBatchStudentStatusCodes.FIXABLE;
      }
      try {
        TypeReference<List<PenRequestBatchStudentValidationIssue>> responseType = new TypeReference<>() {
        };
        var validationResults = new ObjectMapper().readValue(event.getEventPayload(), responseType);
        if (!validationResults.isEmpty()) {
          var mappedEntities = validationResults.stream().map(issueMapper::toModel).collect(Collectors.toList());
          getPenRequestBatchStudentOrchestratorService().saveDemogValidationResultsAndUpdateStudentStatus(mappedEntities, statusCode, sagaData.getPenRequestBatchStudentID());
        }
      } catch (final JsonProcessingException ex) {
        log.error("json exception for :: {} {}", event.getSagaId().toString(), ex);
      }
    }
  }
}
