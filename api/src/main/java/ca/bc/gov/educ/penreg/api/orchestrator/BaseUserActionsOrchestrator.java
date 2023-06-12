package ca.bc.gov.educ.penreg.api.orchestrator;

import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;

import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

/**
 * The type Base user actions orchestrator.
 *
 * @param <T> the type parameter
 */
@Slf4j
public abstract class BaseUserActionsOrchestrator<T> extends BaseOrchestrator<T> {

  /**
   * The constant studentMapper.
   */
  protected static final StudentMapper studentMapper = StudentMapper.mapper;
  /**
   * The constant penRequestBatchStudentMapper.
   */
  protected static final PenRequestBatchStudentMapper penRequestBatchStudentMapper = PenRequestBatchStudentMapper.mapper;

  /**
   * Instantiates a new Base user actions orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   * @param clazz            the clazz
   * @param sagaName         the saga name
   * @param topicToSubscribe the topic to subscribe
   */
  protected BaseUserActionsOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final Class<T> clazz, final String sagaName, final String topicToSubscribe) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
  }

  /**
   * Update PEN Request Batch record and PRB Student record.
   *
   * @param event the event
   * @param saga  the saga
   * @param t     the pen request batch student saga data
   * @throws JsonProcessingException the json processing exception
   */
  protected void updatePenRequestBatchStudent(final Event event, final Saga saga, final T t) throws JsonProcessingException {
    final PenRequestBatchStudent prbStudent = this.createPRBStudent(event, t);
    saga.setSagaState(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    final SagaEvent eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT)
        .replyTo(this.getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(prbStudent))
        .build();
    this.postMessageToTopic(PEN_REQUEST_BATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_BATCH_API_TOPIC for UPDATE_PEN_REQUEST_BATCH_STUDENT Event. :: {}", saga.getSagaId());
  }

  /**
   * Create prb student pen request batch student.
   *
   * @param event the event
   * @param t     the t
   * @return the pen request batch student
   */
  protected abstract PenRequestBatchStudent createPRBStudent(Event event, T t);

  /**
   * Update PEN Request Batch record and PRB Student record.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param basePenRequestBatchStudentSagaData the pen request batch student saga data
   */
  protected void logPenRequestBatchStudentNotFound(final Event event, final Saga saga, final BasePenRequestBatchStudentSagaData basePenRequestBatchStudentSagaData) {
    log.error("Pen request batch student record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
  }
}
