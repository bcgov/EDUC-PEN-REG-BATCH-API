package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.PenMatchSagaMapper;
import ca.bc.gov.educ.penreg.api.mappers.PenStudentDemogValidationMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentOrchestratorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestValidationIssue;
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
import java.util.Optional;
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
   * The Application properties.
   */
  private final ApplicationProperties applicationProperties;
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
   * @param applicationProperties                     the application properties
   * @param penRequestBatchStudentOrchestratorService the pen request batch student orchestrator service
   */
  @Autowired
  public PenReqBatchStudentOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher,
                                        final ApplicationProperties applicationProperties, final PenRequestBatchStudentOrchestratorService penRequestBatchStudentOrchestratorService) {
    super(sagaService, messagePublisher, PenRequestBatchStudentSagaData.class,
        PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString());
    this.applicationProperties = applicationProperties;
    this.setShouldSendNotificationEvent(true);
    this.penRequestBatchStudentOrchestratorService = penRequestBatchStudentOrchestratorService;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
        .begin(VALIDATE_STUDENT_DEMOGRAPHICS, this::validateStudentDemographics)
        .step(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_NO_ERROR_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
        .step(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_WITH_ONLY_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
        .end(VALIDATE_STUDENT_DEMOGRAPHICS, VALIDATION_SUCCESS_WITH_ERROR, this::completePenRequestBatchStudentSaga)
        .or()
        .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
        .end(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED, this::completePenRequestBatchStudentSaga);
  }


  /**
   * Validate student demographics.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void validateStudentDemographics(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws IOException, InterruptedException, TimeoutException {
    final var scrubbedSagaData = this.getPenRequestBatchStudentOrchestratorService()
        .scrubPayload(penRequestBatchStudentSagaData);
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(VALIDATE_STUDENT_DEMOGRAPHICS.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(scrubbedSagaData)); // update the payload with scrubbed values to use it in the saga process...
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final var validationPayload = validationMapper.toStudentDemogValidationPayload(scrubbedSagaData);

    validationPayload.setTransactionID(saga.getSagaId().toString());
    final var eventPayload = JsonUtil.getJsonString(validationPayload);
    if (eventPayload.isPresent()) {
      final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(VALIDATE_STUDENT_DEMOGRAPHICS)
          .replyTo(this.getTopicToSubscribe())
          .eventPayload(eventPayload.get())
          .build();
      this.postMessageToTopic(PEN_SERVICES_API_TOPIC.toString(), nextEvent);
      log.info("message sent to PEN_SERVICES_API_TOPIC for VALIDATE_STUDENT_DEMOGRAPHICS Event. :: {}", saga.getSagaId());
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
  protected void processPenMatchResults(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) throws IOException, InterruptedException, TimeoutException {
    final Optional<Event> eventOptional;
    if (penRequestBatchStudentSagaData.getIsPENMatchResultsProcessed() == null || !penRequestBatchStudentSagaData.getIsPENMatchResultsProcessed()) {
      // this is necessary to check, to avoid duplicate execution during replay process.
      final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
      saga.setSagaState(PROCESS_PEN_MATCH_RESULTS.toString());
      final var penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, event.getEventPayload());
      penRequestBatchStudentSagaData.setPenMatchResult(penMatchResult); // update the original payload with response from PEN_MATCH_API
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData)); // save the updated payload to DB...
      this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

      eventOptional = this.getPenRequestBatchStudentOrchestratorService().processPenMatchResult(saga, penRequestBatchStudentSagaData, penMatchResult);

      penRequestBatchStudentSagaData.setIsPENMatchResultsProcessed(true);
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData)); // save the updated payload to DB...
      this.getSagaService().updateAttachedEntityDuringSagaProcess(saga);
    } else {
      eventOptional = Optional.of(Event.builder().sagaId(saga.getSagaId())
          .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED).eventPayload(penRequestBatchStudentSagaData.getPenMatchResult().getPenStatus())
          .build());
    }

    if (eventOptional.isPresent()) {
      this.handleEvent(eventOptional.get());
    } else {
      this.handleEvent(Event.builder().sagaId(saga.getSagaId())
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
   */
  protected void processPenMatch(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    // need to persist the validation warnings from response payload.
    this.saveDemogValidationResults(event, penRequestBatchStudentSagaData);
    final var eventPayload = JsonUtil.getJsonString(penMatchSagaMapper.toPenMatchStudent(penRequestBatchStudentSagaData));
    if (eventPayload.isPresent()) {
      final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
          .eventType(PROCESS_PEN_MATCH)
          .replyTo(this.getTopicToSubscribe())
          .eventPayload(eventPayload.get())
          .build();
      this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
      log.info("message sent to PEN_MATCH_API_TOPIC for PROCESS_PEN_MATCH Event. :: {}", saga.getSagaId());
    } else {
      log.error("event payload is not present this should not have happened. :: {}", saga.getSagaId());
    }

  }

  /**
   * This method updates the DB and marks the process as complete.
   *
   * @param event    the current event.
   * @param saga     the saga model object.
   * @param sagaData the payload string as object.
   */
  private void completePenRequestBatchStudentSaga(final Event event, final Saga saga, final PenRequestBatchStudentSagaData sagaData) {
    this.saveDemogValidationResults(event, sagaData);
  }

  /**
   * Save demog validation results.
   *
   * @param event    the event
   * @param sagaData the saga data
   */
  protected void saveDemogValidationResults(final Event event, final PenRequestBatchStudentSagaData sagaData) {
    if (event.getEventType() == VALIDATE_STUDENT_DEMOGRAPHICS
        && StringUtils.isNotBlank(event.getEventPayload())
        && !StringUtils.equalsIgnoreCase(VALIDATION_SUCCESS_NO_ERROR_WARNING.toString(), event.getEventPayload())) {
      final PenRequestBatchStudentStatusCodes statusCode;
      if (event.getEventOutcome() == VALIDATION_SUCCESS_WITH_ERROR) {
        statusCode = PenRequestBatchStudentStatusCodes.ERROR;
      } else {
        statusCode = PenRequestBatchStudentStatusCodes.FIXABLE;
      }
      try {
        final TypeReference<List<PenRequestValidationIssue>> responseType = new TypeReference<>() {
        };
        final var validationResults = new ObjectMapper().readValue(event.getEventPayload(), responseType);
        if (!validationResults.isEmpty()) {
          final var mappedEntities = validationResults.stream().map(issueMapper::toModel).collect(Collectors.toList());
          this.getPenRequestBatchStudentOrchestratorService().saveDemogValidationResultsAndUpdateStudentStatus(mappedEntities, statusCode, sagaData.getPenRequestBatchStudentID());
        }
      } catch (final JsonProcessingException ex) {
        log.error("json exception for :: {} {}", event.getSagaId().toString(), ex);
      }
    }
  }
}
