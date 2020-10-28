package ca.bc.gov.educ.penreg.api.service;


import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static ca.bc.gov.educ.penreg.api.constants.EventType.PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventPublisherService {

  @Getter(PRIVATE)
  private final MessagePublisher messagePublisher;

  @Autowired
  public EventPublisherService(final MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void send(PenRequestBatchEvent event) throws JsonProcessingException {
    if (event.getReplyChannel() != null) {
      getMessagePublisher().dispatchMessage(event.getReplyChannel(), penRequestBatchEventProcessed(event));
    }
    getMessagePublisher().dispatchMessage(PEN_REQUEST_BATCH_API_TOPIC.toString(), createOutboxEvent(event));
  }

  private byte[] penRequestBatchEventProcessed(PenRequestBatchEvent penRequestBatchEvent) throws JsonProcessingException {
    Event event = Event.builder()
      .sagaId(penRequestBatchEvent.getSagaId())
      .eventType(EventType.valueOf(penRequestBatchEvent.getEventType()))
      .eventOutcome(EventOutcome.valueOf(penRequestBatchEvent.getEventOutcome()))
      .eventPayload(penRequestBatchEvent.getEventPayload()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }

  private byte[] createOutboxEvent(PenRequestBatchEvent penRequestBatchEvent) throws JsonProcessingException {
    Event event = Event.builder().eventType(PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED).eventPayload(penRequestBatchEvent.getEventId().toString()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }
}
