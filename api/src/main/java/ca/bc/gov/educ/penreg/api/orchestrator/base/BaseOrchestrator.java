package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import io.nats.streaming.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.penreg.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.STARTED;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.STUDENT_API_TOPIC;
import static lombok.AccessLevel.PROTECTED;

/**
 * The type Base orchestrator.
 *
 * @param <T> the type parameter
 */
@Slf4j
public abstract class BaseOrchestrator<T> {
  /**
   * The constant SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT.
   */
  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {}";
  /**
   * The constant API_NAME.
   */
  protected static final String API_NAME = "PEN_REG_BATCH_API";
  /**
   * The Saga service.
   */
  @Getter(PROTECTED)
  private final SagaService sagaService;
  /**
   * The Message publisher.
   */
  @Getter(PROTECTED)
  private final MessagePublisher messagePublisher;
  /**
   * The Clazz.
   */
  protected final Class<T> clazz;

  /**
   * The Saga name.
   */
  @Getter(PROTECTED)
  private final String sagaName;
  /**
   * The Topic to subscribe.
   */
  @Getter(PROTECTED)
  private final String topicToSubscribe;

  /**
   * The Next steps to execute.
   */
  protected final Map<EventType, List<SagaEventState<T>>> nextStepsToExecute = new LinkedHashMap<>();

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService       the saga service
   * @param messagePublisher  the message publisher
   * @param messageSubscriber the message subscriber
   * @param taskScheduler     the task scheduler
   * @param clazz             the clazz
   * @param sagaName          the saga name
   * @param topicToSubscribe  the topic to subscribe
   */
  public BaseOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler, Class<T> clazz, String sagaName, String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    messageSubscriber.subscribe(topicToSubscribe, this::executeSagaEvent);
    taskScheduler.registerSagaOrchestrators(sagaName, this);
    populateStepsToExecuteMap();
  }

  /**
   * Create single collection event state list.
   *
   * @param eventOutcome  the event outcome
   * @param nextEventType the next event type
   * @param stepToExecute the step to execute
   * @return the list
   */
  protected List<SagaEventState<T>> createSingleCollectionEventState(EventOutcome eventOutcome, EventType nextEventType, SagaStep<T> stepToExecute) {
    List<SagaEventState<T>> eventStates = new ArrayList<>();
    eventStates.add(buildSagaEventState(eventOutcome, nextEventType, stepToExecute));
    return eventStates;
  }


  /**
   * Build saga event state saga event state.
   *
   * @param eventOutcome  the event outcome
   * @param nextEventType the next event type
   * @param stepToExecute the step to execute
   * @return the saga event state
   */
  protected SagaEventState<T> buildSagaEventState(EventOutcome eventOutcome, EventType nextEventType, SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
  }


  /**
   * Register step to execute base orchestrator.
   *
   * @param initEvent     the init event
   * @param outcome       the outcome
   * @param nextEvent     the next event
   * @param stepToExecute the step to execute
   * @return the base orchestrator
   */
  protected BaseOrchestrator<T> registerStepToExecute(EventType initEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    if (this.nextStepsToExecute.containsKey(initEvent)) {
      List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(buildSagaEventState(outcome, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, createSingleCollectionEventState(outcome, nextEvent, stepToExecute));
    }
    return this;
  }

  /**
   * Step base orchestrator.
   *
   * @param currentEvent  the event that has occurred.
   * @param outcome       outcome of the event.
   * @param nextEvent     next event that will occur.
   * @param stepToExecute which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> step(EventType currentEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    return registerStepToExecute(currentEvent, outcome, nextEvent, stepToExecute);
  }

  /**
   * this is a simple and convenient method to trigger builder pattern in the child classes.
   *
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> stepBuilder() {
    return this;
  }

  /**
   * this method will check if the event is not already processed. this could happen in SAGAs due to duplicate messages.
   * Application should be able to handle this.
   *
   * @param currentEventType current event.
   * @param saga             the model object.
   * @param eventTypes       event types stored in the hashmap
   * @return true or false based on whether the current event with outcome received from the queue is already processed or not.
   */
  protected boolean isNotProcessedEvent(EventType currentEventType, Saga saga, Set<EventType> eventTypes) {
    EventType eventTypeInDB = EventType.valueOf(saga.getSagaState());
    List<EventType> events = new LinkedList<>(eventTypes);
    int dbEventIndex = events.indexOf(eventTypeInDB);
    int currentEventIndex = events.indexOf(currentEventType);
    return currentEventIndex >= dbEventIndex;
  }

  /**
   * creates the PenRequestSagaEventState object
   *
   * @param saga         the payload.
   * @param eventType    event type
   * @param eventOutcome outcome
   * @param eventPayload payload.
   * @return {@link SagaEvent}
   */
  protected SagaEvent createEventState(@NotNull Saga saga, @NotNull EventType eventType, @NotNull EventOutcome eventOutcome, String eventPayload) {
    var user = sagaName.length() > 32 ? sagaName.substring(0, 32) : sagaName;
    return SagaEvent.builder()
      .createDate(LocalDateTime.now())
      .createUser(user)
      .updateDate(LocalDateTime.now())
      .updateUser(user)
      .saga(saga)
      .sagaEventOutcome(eventOutcome.toString())
      .sagaEventState(eventType.toString())
      .sagaStepNumber(calculateStep(saga))
      .sagaEventResponse(eventPayload == null ? "" : eventPayload)
      .build();
  }

  /**
   * This method updates the DB and marks the process as complete.
   *
   * @param event    the current event.
   * @param saga     the saga model object.
   * @param sagaData the payload string as object.
   */
  protected void markSagaComplete(Event event, Saga saga, T sagaData) {
    log.trace("payload is {}", sagaData);
    SagaEvent sagaEvent = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(COMPLETED.toString());
    saga.setStatus(COMPLETED.toString());
    saga.setUpdateDate(LocalDateTime.now());
    getSagaService().updateAttachedSagaWithEvents(saga, sagaEvent);

  }

  /**
   * calculate step number
   *
   * @param saga the model object.
   * @return step number that was calculated.
   */
  private int calculateStep(Saga saga) {
    val sagaStates = getSagaService().findAllSagaStates(saga);
    return (sagaStates.size() + 1);
  }

  /**
   * convenient method to post message to topic, to be used by child classes.
   *
   * @param topicName topic name where the message will be posted.
   * @param nextEvent the next event object.
   */
  protected void postMessageToTopic(String topicName, Event nextEvent) {
    var eventStringOptional = JsonUtil.getJsonString(nextEvent);
    if (eventStringOptional.isPresent()) {
      getMessagePublisher().dispatchMessage(topicName, eventStringOptional.get().getBytes());
    } else {
      log.error("event string is not present for  :: {} :: this should not have happened", nextEvent);
    }
  }

  /**
   * it finds the last event that was processed successfully for this saga.
   *
   * @param eventStates event states corresponding to the Saga.
   * @return {@link SagaEvent} if found else null.
   */
  protected Optional<SagaEvent> findTheLastEventOccurred(List<SagaEvent> eventStates) {
    int step = eventStates.stream().map(SagaEvent::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
    return eventStates.stream().filter(element -> element.getSagaStepNumber() == step).findFirst();
  }

  /**
   * this method is called from the cron job , which will replay the saga process based on its current state.
   *
   * @param saga the model object.
   * @throws IOException          if there is connectivity problem
   * @throws InterruptedException if thread is interrupted.
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Async
  @Transactional
  public void replaySaga(Saga saga) throws IOException, InterruptedException, TimeoutException {
    var eventStates = getSagaService().findAllSagaStates(saga);
    T t = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      replayFromBeginning(saga, t);
    } else {
      replayFromLastEvent(saga, eventStates, t);
    }
  }

  /**
   * This method will restart the saga process from where it was left the last time. which could occur due to various reasons
   *
   * @param saga        the model object.
   * @param eventStates the event states corresponding to the saga
   * @param t           the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws TimeoutException     if connection to messaging system times out.
   * @throws IOException          if there is connectivity problem
   */
  private void replayFromLastEvent(Saga saga, List<SagaEvent> eventStates, T t) throws InterruptedException, TimeoutException, IOException {
    val sagaEventOptional = findTheLastEventOccurred(eventStates);
    if (sagaEventOptional.isPresent()) {
      val sagaEvent = sagaEventOptional.get();
      log.trace(sagaEventOptional.toString());
      EventType currentEvent = EventType.valueOf(sagaEvent.getSagaEventState());
      EventOutcome eventOutcome = EventOutcome.valueOf(sagaEvent.getSagaEventOutcome());
      Event event = Event.builder()
        .eventOutcome(eventOutcome)
        .eventType(currentEvent)
        .eventPayload(sagaEvent.getSagaEventResponse())
        .build();
      Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(currentEvent, eventOutcome);
      if (sagaEventState.isPresent()) {
        log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
        invokeNextEvent(event, saga, t, sagaEventState.get());
      }
    }
  }

  /**
   * This method will restart the saga process from the beginning. which could occur due to various reasons
   *
   * @param saga the model object.
   * @param t    the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws TimeoutException     if connection to messaging system times out.
   * @throws IOException          if there is connectivity problem
   */
  private void replayFromBeginning(Saga saga, T t) throws InterruptedException, TimeoutException, IOException {
    Event event = Event.builder()
      .eventOutcome(INITIATE_SUCCESS)
      .eventType(INITIATED)
      .build();
    Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(INITIATED, INITIATE_SUCCESS);
    if (sagaEventState.isPresent()) {
      log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
      invokeNextEvent(event, saga, t, sagaEventState.get());
    }
  }

  /**
   * this method is called if there is a new message on this specific topic which this service is listening.
   *
   * @param message the event in the topic received as a json string and then converted to {@link Message}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Async
  @Transactional
  public void executeSagaEvent(@NotNull Message message) throws InterruptedException, IOException, TimeoutException {
    Event event = JsonUtil.getJsonObjectFromByteArray(Event.class, message.getData());
    Optional<Saga> sagaOptional;
    log.trace("executing saga event {}", event);
    if (event.getEventType() == EventType.READ_FROM_TOPIC && event.getEventOutcome() == EventOutcome.READ_FROM_TOPIC_SUCCESS) {
      PenRequestBatchStudentSagaData penRequestBatchStudentSagaData = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentSagaData.class, event.getEventPayload());
      sagaOptional = getSagaService().findByPenRequestBatchStudentID(penRequestBatchStudentSagaData.getPenRequestBatchStudentID());
      if (sagaOptional.isPresent()) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
        message.ack();
        return;
      }
      sagaOptional = Optional.of(createSagaRecordInDB(event.getEventPayload(), penRequestBatchStudentSagaData.getPenRequestBatchStudentID()));

    } else {
      sagaOptional = getSagaService().findSagaById(event.getSagaId()); // system expects a saga record to be present here.
    }
    if (sagaOptional.isPresent()) {
      val saga = sagaOptional.get();
      if (!COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())) {//possible duplicate message or force stop scenario check
        var sagaEventState = findNextSagaEventState(event.getEventType(), event.getEventOutcome());
        log.trace("found next event as {}", sagaEventState);
        if (sagaEventState.isPresent()) {
          process(event, saga, sagaEventState.get());
        } else {
          log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
        }
      } else {
        log.info("got message to process saga for saga ID :: {} but saga is already :: {}", saga.getSagaId(), saga.getStatus());
      }
    } else {
      log.error("Saga process without DB record is not expected. {}", event);
    }
    message.ack(); // manual acknowledgement to STAN.
  }

  /**
   * Create saga record in db saga.
   *
   * @param payload                  the payload
   * @param penRequestBatchStudentID the pen request batch student id
   * @return the saga
   */
  private Saga createSagaRecordInDB(String payload, UUID penRequestBatchStudentID) {
    var saga = Saga
      .builder()
      .payload(payload)
      .penRequestBatchStudentID(penRequestBatchStudentID)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .createDate(LocalDateTime.now())
      .createUser(API_NAME)
      .updateUser(API_NAME)
      .updateDate(LocalDateTime.now())
      .build();
    return getSagaService().createSagaRecord(saga);
  }


  /**
   * this method finds the next event that needs to be executed.
   *
   * @param currentEvent current event
   * @param eventOutcome event outcome.
   * @return {@link Optional<SagaEventState>}
   */
  protected Optional<SagaEventState<T>> findNextSagaEventState(EventType currentEvent, EventOutcome eventOutcome) {
    val sagaEventStates = nextStepsToExecute.get(currentEvent);
    return sagaEventStates == null ? Optional.empty() : sagaEventStates.stream().filter(el -> el.getCurrentEventOutcome() == eventOutcome).findFirst();
  }

  /**
   * this method starts the process of saga event execution.
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws TimeoutException     if connection to messaging system times out.
   * @throws IOException          if there is connectivity problem
   */
  protected void process(@NotNull Event event, Saga saga, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    T sagaData = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (!saga.getSagaState().equalsIgnoreCase(COMPLETED.toString())
      && isNotProcessedEvent(event.getEventType(), saga, this.nextStepsToExecute.keySet())) {
      log.info(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.getNextEventType(), event.toString());
      invokeNextEvent(event, saga, sagaData, sagaEventState);
    } else {
      log.info("ignoring this message as we have already processed it or it is completed. {}", event.toString()); // it is expected to receive duplicate message in saga pattern, system should be designed to handle duplicates.
    }
  }

  /**
   * this method will invoke the next event in the {@link BaseOrchestrator#nextStepsToExecute}
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaData       the payload string
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws TimeoutException     if connection to messaging system times out.
   * @throws IOException          if there is connectivity problem
   */
  protected void invokeNextEvent(Event event, Saga saga, T sagaData, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }

  /**
   * Populate steps to execute map.
   */
  public abstract void populateStepsToExecuteMap();

  /**
   * Delegate message posting for student.
   *
   * @param saga      the saga
   * @param student   the student
   * @param eventType the event type
   */
  protected void delegateMessagePostingForStudent(Saga saga, Student student, EventType eventType) {
    var eventPayloadOptional = JsonUtil.getJsonString(student);
    if (eventPayloadOptional.isPresent()) {
      Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(eventType)
        .replyTo(getTopicToSubscribe())
        .eventPayload(eventPayloadOptional.get())
        .build();
      postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    } else {
      log.error("event payload is not present this should not have happened.");
    }
  }
}
