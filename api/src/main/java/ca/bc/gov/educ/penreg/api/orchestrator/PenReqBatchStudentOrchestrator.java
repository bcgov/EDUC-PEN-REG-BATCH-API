package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentOrchestratorService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenMatchResult;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen req batch student orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchStudentOrchestrator extends BaseOrchestrator<PenRequestBatchStudentSagaData> {

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
   * @param taskScheduler                             the task scheduler
   * @param penRequestBatchStudentOrchestratorService the pen request batch student orchestrator service
   */
  @Autowired
  public PenReqBatchStudentOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                        EventTaskScheduler taskScheduler,
                                        PenRequestBatchStudentOrchestratorService penRequestBatchStudentOrchestratorService) {
    super(sagaService, messagePublisher, taskScheduler, PenRequestBatchStudentSagaData.class, PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString());
    this.penRequestBatchStudentOrchestratorService = penRequestBatchStudentOrchestratorService;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .step(READ_FROM_TOPIC, READ_FROM_TOPIC_SUCCESS, PROCESS_PEN_MATCH, this::processPenMatch)
        .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
        .step(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED, MARK_SAGA_COMPLETE, this::markSagaComplete);
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
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
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
   */
  private void processPenMatch(final Event event, final Saga saga, final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    var eventPayload = JsonUtil.getJsonString(penRequestBatchStudentSagaData);
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
}
