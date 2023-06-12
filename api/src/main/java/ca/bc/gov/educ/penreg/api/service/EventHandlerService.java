package ca.bc.gov.educ.penreg.api.service;


import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchStudentOrchestrator;
import ca.bc.gov.educ.penreg.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerService implements EventHandler {

  /**
   * The constant PAYLOAD_LOG.
   */
  public static final String PAYLOAD_LOG = "payload is :: {}";
  /**
   * The Saga service.
   */
  @Getter(PROTECTED)
  private final SagaService sagaService;
  /**
   * The Pen req batch student orchestrator.
   */
  @Getter(PROTECTED)
  private final PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator;

  /**
   * The Pen request batch event repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchEventRepository penRequestBatchEventRepository;

  /**
   * The Prb student event service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchEventService penRequestBatchEventService;

  /**
   * The Event publisher service.
   */
  @Getter(PRIVATE)
  private final EventPublisherService eventPublisherService;

  /**
   * Instantiates a new Event handler service.
   *
   * @param sagaService                    the saga service
   * @param penReqBatchStudentOrchestrator the pen req batch student orchestrator
   * @param penRequestBatchEventRepository the pen request batch event repository
   * @param penRequestBatchEventService         the prb student event service
   * @param eventPublisherService          the event publisher service
   */
  @Autowired
  public EventHandlerService(final SagaService sagaService,
                             final PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator,
                             final PenRequestBatchEventRepository penRequestBatchEventRepository,
                             final PenRequestBatchEventService penRequestBatchEventService,
                             final EventPublisherService eventPublisherService) {
    this.penRequestBatchEventService = penRequestBatchEventService;
    this.penRequestBatchEventRepository = penRequestBatchEventRepository;
    this.sagaService = sagaService;
    this.penReqBatchStudentOrchestrator = penReqBatchStudentOrchestrator;
    this.eventPublisherService = eventPublisherService;
  }

  /**
   * Handle event.
   *
   * @param event the event
   */
  @Override
  @Async("subscriberExecutor")
  public void handleEvent(final Event event) {
    try {
      switch (event.getEventType()) {
        case READ_FROM_TOPIC:
          log.info("received read from topic event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          this.handleReadFromTopicEvent(event);
          break;
        case UPDATE_PEN_REQUEST_BATCH_STUDENT:
          log.info("received update pen request batch student event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          this.handleUpdatePrbStudentEvent(event);
          break;
        case ARCHIVE_PEN_REQUEST_BATCH:
          log.info("received archive pen request batch event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          this.handleArchivePenRequestBatchEvent(event);
          break;
        default:
          log.info("silently ignoring other events.");
          break;
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  /**
   * Gets topic to subscribe.
   *
   * @return the topic to subscribe
   */
  @Override
  public String getTopicToSubscribe() {
    return PEN_REQUEST_BATCH_API_TOPIC.toString();
  }

  /**
   * Saga should never be null for this type of event.
   *
   * @param event containing the student PEN.
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  private void handleReadFromTopicEvent(final Event event) throws InterruptedException, TimeoutException, IOException {
    if (event.getEventOutcome() == EventOutcome.READ_FROM_TOPIC_SUCCESS) {
      final PenRequestBatchStudentSagaData penRequestBatchStudentSagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, event.getEventPayload());
      final var sagaList = this.getSagaService().findByPenRequestBatchStudentIDAndSagaName(penRequestBatchStudentSagaData.getPenRequestBatchStudentID(),
          PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString());
      if (sagaList.size() > 0) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
        return;
      }

      var saga = this.getPenReqBatchStudentOrchestrator().createSaga(event.getEventPayload(), penRequestBatchStudentSagaData.getPenRequestBatchStudentID(),
        penRequestBatchStudentSagaData.getPenRequestBatchID(), ApplicationProperties.API_NAME);
      this.getPenReqBatchStudentOrchestrator().startSaga(saga);
    }
  }


  /**
   * Send event immediately after update PrbStudent. The scheduler will resend it if failed.
   * Make sure that the PrbStudent update and event sending are in different transactions,
   * so the failure to send event would not affect PrbStudent update.
   *
   * @param event the update PrbStudent event
   * @throws JsonProcessingException the json processing exception
   */
  private void handleUpdatePrbStudentEvent(final Event event) throws JsonProcessingException {
    final PenRequestBatchEvent penRequestBatchEvent = this.getPenRequestBatchEventService().updatePenRequestBatchStudent(event);
    this.getEventPublisherService().send(penRequestBatchEvent);
  }

  /**
   * Handle archive pen request batch event
   *
   * @param event the archive pen request batch event
   * @throws JsonProcessingException the json processing exception
   */
  private void handleArchivePenRequestBatchEvent(final Event event) throws JsonProcessingException {
    final PenRequestBatchEvent penRequestBatchEvent = this.getPenRequestBatchEventService().archivePenRequestBatch(event);
    this.getEventPublisherService().send(penRequestBatchEvent);
  }

}
