package ca.bc.gov.educ.penreg.api.service;


import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Service
@Slf4j
public class EventHandlerService implements EventHandler {

  public static final String NO_RECORD_SAGA_ID_EVENT_TYPE = "no record found for the saga id and event type combination, processing.";
  public static final String RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE = "record found for the saga id and event type combination, might be a duplicate or replay," +
      " just updating the db status so that it will be polled and sent back again.";
  public static final String PAYLOAD_LOG = "payload is :: {}";
  public static final String EVENT_PAYLOAD = "event is :: {}";
  /**
   * The Saga service.
   */
  @Getter(PROTECTED)
  private final SagaService sagaService;
  @Getter(PROTECTED)
  private final PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator;

  @Getter(PRIVATE)
  private final PenRequestBatchEventRepository penRequestBatchEventRepository;

  @Getter(PRIVATE)
  private final PenRequestBatchStudentEventService prbStudentEventService;

  @Getter(PRIVATE)
  private final EventPublisherService eventPublisherService;

  @Autowired
  public EventHandlerService(final SagaService sagaService, final MessageSubscriber messageSubscriber,
                             final PenReqBatchStudentOrchestrator penReqBatchStudentOrchestrator,
                             final PenRequestBatchEventRepository penRequestBatchEventRepository,
                             final PenRequestBatchStudentEventService prbStudentEventService, final EventPublisherService eventPublisherService) {
    this.prbStudentEventService = prbStudentEventService;
    this.penRequestBatchEventRepository = penRequestBatchEventRepository;
    this.sagaService = sagaService;
    this.penReqBatchStudentOrchestrator = penReqBatchStudentOrchestrator;
    this.eventPublisherService = eventPublisherService;
    messageSubscriber.subscribe(PEN_REQUEST_BATCH_API_TOPIC.toString(), this);
  }

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
   * Saga should never be null for this type of event.
   *
   * @param event containing the student PEN.
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
   *  Send event immediately after update PrbStudent. The scheduler will resend it if failed.
   *  Make sure that the PrbStudent update and event sending are in different transactions,
   *  so the failure to send event would not affect PrbStudent update.
   *
   * @param event the update PrbStudent event
   * @throws JsonProcessingException
   */
  private void handleUpdatePrbStudentEvent(Event event) throws JsonProcessingException {
    PenRequestBatchEvent penRequestBatchEvent = getPrbStudentEventService().updatePenRequestBatchStudent(event);
    getEventPublisherService().send(penRequestBatchEvent);
  }

  private void handlePenRequestBatchOutboxProcessedEvent(String eventId) {
    val eventFromDB = getPenRequestBatchEventRepository().findById(UUID.fromString(eventId));
    if (eventFromDB.isPresent()) {
      val studEvent = eventFromDB.get();
      studEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
      getPenRequestBatchEventRepository().save(studEvent);
    }
  }

}
