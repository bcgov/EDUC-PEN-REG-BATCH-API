package ca.bc.gov.educ.penreg.api.service;


import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
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
  public void send(final PenRequestBatchEvent event) throws JsonProcessingException {
    if (event.getReplyChannel() != null) {
      this.getMessagePublisher().dispatchMessage(event.getReplyChannel(), this.penRequestBatchEventProcessed(event));
    }
    this.getMessagePublisher().dispatchMessage(PEN_REQUEST_BATCH_API_TOPIC.toString(), this.createOutboxEvent(event));
  }

  private byte[] penRequestBatchEventProcessed(final PenRequestBatchEvent penRequestBatchEvent) throws JsonProcessingException {
    final Event event = Event.builder()
        .sagaId(penRequestBatchEvent.getSagaId())
        .eventType(EventType.valueOf(penRequestBatchEvent.getEventType()))
        .eventOutcome(EventOutcome.valueOf(penRequestBatchEvent.getEventOutcome()))
        .eventPayload(penRequestBatchEvent.getEventPayload()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }

  private byte[] createOutboxEvent(final PenRequestBatchEvent penRequestBatchEvent) throws JsonProcessingException {
    final Event event = Event.builder().eventType(PEN_REQUEST_BATCH_EVENT_OUTBOX_PROCESSED).eventPayload(penRequestBatchEvent.getEventId().toString()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }
}
