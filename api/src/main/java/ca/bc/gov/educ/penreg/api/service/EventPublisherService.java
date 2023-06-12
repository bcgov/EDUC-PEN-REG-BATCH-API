package ca.bc.gov.educ.penreg.api.service;


import static lombok.AccessLevel.PRIVATE;

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
  }

  private byte[] penRequestBatchEventProcessed(final PenRequestBatchEvent penRequestBatchEvent) throws JsonProcessingException {
    final Event event = Event.builder()
        .sagaId(penRequestBatchEvent.getSagaId())
        .eventType(EventType.valueOf(penRequestBatchEvent.getEventType()))
        .eventOutcome(EventOutcome.valueOf(penRequestBatchEvent.getEventOutcome()))
        .eventPayload(penRequestBatchEvent.getEventPayload()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }

}
