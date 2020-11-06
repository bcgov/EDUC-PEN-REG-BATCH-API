package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import static ca.bc.gov.educ.penreg.api.constants.EventType.UPDATE_PEN_REQUEST_BATCH_STUDENT;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;

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
  protected BaseUserActionsOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, Class<T> clazz, String sagaName, String topicToSubscribe) {
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
  protected void updatePenRequestBatchStudent(Event event, Saga saga, T t) throws JsonProcessingException {
    PenRequestBatchStudent prbStudent = updateSagaDataAndCreatePRBStudent(event, t);
    saga.setSagaState(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(t));
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                           .eventType(UPDATE_PEN_REQUEST_BATCH_STUDENT)
                           .replyTo(getTopicToSubscribe())
                           .eventPayload(JsonUtil.getJsonStringFromObject(prbStudent))
                           .build();
    postMessageToTopic(PEN_REQUEST_BATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_BATCH_API_TOPIC for UPDATE_PEN_REQUEST_BATCH_STUDENT Event. :: {}", saga.getSagaId());
  }

  /**
   * Update saga data and create prb student pen request batch student.
   *
   * @param event the event
   * @param t     the t
   * @return the pen request batch student
   * @throws JsonProcessingException the json processing exception
   */
  protected abstract PenRequestBatchStudent updateSagaDataAndCreatePRBStudent(Event event, T t) throws JsonProcessingException;

  /**
   * Update PEN Request Batch record and PRB Student record.
   *
   * @param event                              the event
   * @param saga                               the saga
   * @param penRequestBatchUserActionsSagaData the pen request batch student saga data
   */
  protected void logPenRequestBatchStudentNotFound(Event event, Saga saga, PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    log.error("Pen request batch student record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
  }
}
