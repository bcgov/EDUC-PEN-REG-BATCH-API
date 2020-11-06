package ca.bc.gov.educ.penreg.api.service;


import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchStudentOrchestrator;
import ca.bc.gov.educ.penreg.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

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
  private final PenRequestBatchStudentEventService prbStudentEventService;

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
   * @param prbStudentEventService         the prb student event service
   * @param eventPublisherService          the event publisher service
   */
  @Autowired
  public EventHandlerService(final SagaService sagaService,
                             final PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator,
                             final PenRequestBatchEventRepository penRequestBatchEventRepository,
                             final PenRequestBatchStudentEventService prbStudentEventService,
                             final EventPublisherService eventPublisherService) {
    this.prbStudentEventService = prbStudentEventService;
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
  @Async("subscriberExecutor")
  public void handleEvent(Event event) {
    try {
      switch (event.getEventType()) {
        case PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED:
          log.info("received outbox processed event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handlePenRequestBatchOutboxProcessedEvent(event.getEventPayload());
          break;
        case READ_FROM_TOPIC:
          log.info("received read from topic event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleReadFromTopicEvent(event);
          break;
        case UPDATE_PEN_REQUEST_BATCH_STUDENT:
          log.info("received update pen request batch student event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleUpdatePrbStudentEvent(event);
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
  private void handleReadFromTopicEvent(Event event) throws InterruptedException, TimeoutException, IOException {
    if (event.getEventOutcome() == EventOutcome.READ_FROM_TOPIC_SUCCESS) {
      PenRequestBatchStudentSagaData penRequestBatchStudentSagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, event.getEventPayload());
      var sagaList = getSagaService().findByPenRequestBatchStudentIDAndSagaName(penRequestBatchStudentSagaData.getPenRequestBatchStudentID(),
          PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString());
      if (sagaList.size() > 0) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
        return;
      }

      getPenReqBatchStudentOrchestrator().startSaga(event.getEventPayload(), penRequestBatchStudentSagaData.getPenRequestBatchStudentID(),
          penRequestBatchStudentSagaData.getPenRequestBatchID());
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
  private void handleUpdatePrbStudentEvent(Event event) throws JsonProcessingException {
    PenRequestBatchEvent penRequestBatchEvent = getPrbStudentEventService().updatePenRequestBatchStudent(event);
    getEventPublisherService().send(penRequestBatchEvent);
  }

  /**
   * Handle pen request batch outbox processed event.
   *
   * @param eventId the event id
   */
  private void handlePenRequestBatchOutboxProcessedEvent(String eventId) {
    val eventFromDB = getPenRequestBatchEventRepository().findById(UUID.fromString(eventId));
    if (eventFromDB.isPresent()) {
      val studEvent = eventFromDB.get();
      studEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
      getPenRequestBatchEventRepository().save(studEvent);
    }
  }

}
