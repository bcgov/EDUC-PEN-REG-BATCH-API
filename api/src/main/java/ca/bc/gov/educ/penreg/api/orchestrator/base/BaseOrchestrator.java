package ca.bc.gov.educ.penreg.api.orchestrator.base;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.mappers.PenRequestBatchStudentValidationIssueMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.NotificationEvent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import ca.bc.gov.educ.penreg.api.util.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.SAGA_COMPLETED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.penreg.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static lombok.AccessLevel.PROTECTED;
import static lombok.AccessLevel.PUBLIC;

/**
 * The type Base orchestrator.
 *
 * @param <T> the type parameter
 */
@Slf4j
public abstract class BaseOrchestrator<T> implements EventHandler, Orchestrator {

  private final Executor multipleSagaExecutor = new EnhancedQueueExecutor.Builder()
          .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("multiple-saga-executor-%d").get())
          .setCorePoolSize(1).setMaximumPoolSize(5).setKeepAliveTime(Duration.ofSeconds(60)).build();
  /**
   * The constant issueMapper.
   */
  protected static final PenRequestBatchStudentValidationIssueMapper issueMapper = PenRequestBatchStudentValidationIssueMapper.mapper;
  /**
   * The constant SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT.
   */
  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {} and SAGA ID :: {}";
  /**
   * The constant SELF
   */
  protected static final String SELF = "SELF";
  /**
   * The flag to indicate whether t
   */
  @Setter(PROTECTED)
  protected boolean shouldSendNotificationEvent = true;
  /**
   * The Clazz.
   */
  protected final Class<T> clazz;
  /**
   * The Next steps to execute.
   */
  protected final Map<EventType, List<SagaEventState<T>>> nextStepsToExecute = new LinkedHashMap<>();
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
   * The Saga name.
   */
  @Getter(PUBLIC)
  private final String sagaName;
  /**
   * The Topic to subscribe.
   */
  @Getter(PUBLIC)
  private final String topicToSubscribe;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   * @param clazz            the clazz
   * @param sagaName         the saga name
   * @param topicToSubscribe the topic to subscribe
   */
  protected BaseOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher,
                             final Class<T> clazz, final String sagaName,
                             final String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    this.populateStepsToExecuteMap();
  }

  /**
   * Create single collection event state list.
   *
   * @param eventOutcome  the event outcome
   * @param nextEventType the next event type
   * @param stepToExecute the step to execute
   * @return the list
   */
  protected List<SagaEventState<T>> createSingleCollectionEventState(final EventOutcome eventOutcome, final Predicate<T> nextStepPredicate, final EventType nextEventType, final SagaStep<T> stepToExecute) {
    final List<SagaEventState<T>> eventStates = new ArrayList<>();
    eventStates.add(this.buildSagaEventState(eventOutcome, nextStepPredicate, nextEventType, stepToExecute));
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
  protected SagaEventState<T> buildSagaEventState(final EventOutcome eventOutcome, final Predicate<T> nextStepPredicate, final EventType nextEventType, final SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).nextStepPredicate(nextStepPredicate).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
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
  protected BaseOrchestrator<T> registerStepToExecute(final EventType initEvent, final EventOutcome outcome, final Predicate<T> nextStepPredicate, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    if (this.nextStepsToExecute.containsKey(initEvent)) {
      final List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(this.buildSagaEventState(outcome, nextStepPredicate, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, this.createSingleCollectionEventState(outcome, nextStepPredicate, nextEvent, stepToExecute));
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
  public BaseOrchestrator<T> step(final EventType currentEvent, final EventOutcome outcome, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(currentEvent, outcome, (T sagaData) -> true, nextEvent, stepToExecute);
  }

  /**
   * Step base orchestrator.
   *
   * @param currentEvent      the event that has occurred.
   * @param outcome           outcome of the event.
   * @param nextStepPredicate whether to execute the next step.
   * @param nextEvent         next event that will occur.
   * @param stepToExecute     which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> step(final EventType currentEvent, final EventOutcome outcome, final Predicate<T> nextStepPredicate, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(currentEvent, outcome, nextStepPredicate, nextEvent, stepToExecute);
  }

  /**
   * Beginning step base orchestrator.
   *
   * @param nextEvent     next event that will occur.
   * @param stepToExecute which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> begin(final EventType nextEvent, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(INITIATED, INITIATE_SUCCESS, (T sagaData) -> true, nextEvent, stepToExecute);
  }

  /**
   * Beginning step base orchestrator.
   *
   * @param nextEvent         next event that will occur.
   * @param nextStepPredicate whether to execute the next step.
   * @param stepToExecute     which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> begin(final Predicate<T> nextStepPredicate, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(INITIATED, INITIATE_SUCCESS, nextStepPredicate, nextEvent, stepToExecute);
  }

  /**
   * End step base orchestrator with complete status.
   *
   * @param currentEvent the event that has occurred.
   * @param outcome      outcome of the event.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> end(final EventType currentEvent, final EventOutcome outcome) {
    return this.registerStepToExecute(currentEvent, outcome, (T sagaData) -> true, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * End step with method to execute with complete status.
   *
   * @param currentEvent  the event that has occurred.
   * @param outcome       outcome of the event.
   * @param stepToExecute which method to execute for the MARK_SAGA_COMPLETE event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> end(final EventType currentEvent, final EventOutcome outcome, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(currentEvent, outcome, (T sagaData) -> true, MARK_SAGA_COMPLETE, (Event event, Saga saga, T sagaData) -> {
      stepToExecute.apply(event, saga, sagaData);
      this.markSagaComplete(event, saga, sagaData);
    });
  }

  /**
   * Syntax sugar to make the step statement expressive
   *
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> or() {
    return this;
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
  protected boolean isNotProcessedEvent(final EventType currentEventType, final Saga saga, final Set<EventType> eventTypes) {
    final EventType eventTypeInDB = EventType.valueOf(saga.getSagaState());
    final List<EventType> events = new LinkedList<>(eventTypes);
    final int dbEventIndex = events.indexOf(eventTypeInDB);
    final int currentEventIndex = events.indexOf(currentEventType);
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
  protected SagaEvent createEventState(@NotNull final Saga saga, @NotNull final EventType eventType, @NotNull final EventOutcome eventOutcome, final String eventPayload) {
    final var user = this.sagaName.length() > 32 ? this.sagaName.substring(0, 32) : this.sagaName;
    return SagaEvent.builder()
        .createDate(LocalDateTime.now())
        .createUser(user)
        .updateDate(LocalDateTime.now())
        .updateUser(user)
        .saga(saga)
        .sagaEventOutcome(eventOutcome.toString())
        .sagaEventState(eventType.toString())
        .sagaStepNumber(this.calculateStep(saga))
        .sagaEventResponse(eventPayload == null ? " " : eventPayload)
        .build();
  }

  /**
   * This method updates the DB and marks the process as complete.
   *
   * @param event    the current event.
   * @param saga     the saga model object.
   * @param sagaData the payload string as object.
   */
  protected void markSagaComplete(final Event event, final Saga saga, final T sagaData) {
    log.trace("payload is {}", sagaData);
    if (this.shouldSendNotificationEvent) {
      final var finalEvent = new NotificationEvent();
      BeanUtils.copyProperties(event, finalEvent);
      finalEvent.setEventType(MARK_SAGA_COMPLETE);
      finalEvent.setEventOutcome(SAGA_COMPLETED);
      finalEvent.setSagaStatus(COMPLETED.toString());
      finalEvent.setSagaName(this.getSagaName());
      this.postMessageToTopic(this.getTopicToSubscribe(), finalEvent);
    }

    final SagaEvent sagaEvent = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(COMPLETED.toString());
    saga.setStatus(COMPLETED.toString());
    saga.setUpdateDate(LocalDateTime.now());
    this.getSagaService().updateAttachedSagaWithEvents(saga, sagaEvent);

  }

  /**
   * calculate step number
   *
   * @param saga the model object.
   * @return step number that was calculated.
   */
  private int calculateStep(final Saga saga) {
    val sagaStates = this.getSagaService().findAllSagaStates(saga);
    return (sagaStates.size() + 1);
  }

  /**
   * convenient method to post message to topic, to be used by child classes.
   *
   * @param topicName topic name where the message will be posted.
   * @param nextEvent the next event object.
   */
  protected void postMessageToTopic(final String topicName, final Event nextEvent) {
    final var eventStringOptional = JsonUtil.getJsonString(nextEvent);
    if (eventStringOptional.isPresent()) {
      this.getMessagePublisher().dispatchMessage(topicName, eventStringOptional.get().getBytes());
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
  protected Optional<SagaEvent> findTheLastEventOccurred(final List<SagaEvent> eventStates) {
    final int step = eventStates.stream().map(SagaEvent::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
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
  @Override
  @Async
  @Transactional
  public void replaySaga(final Saga saga) throws IOException, InterruptedException, TimeoutException {
    final var eventStates = this.getSagaService().findAllSagaStates(saga);
    final var t = JsonUtil.getJsonObjectFromString(this.clazz, saga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      this.replayFromBeginning(saga, t);
    } else {
      this.replayFromLastEvent(saga, eventStates, t);
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
  private void replayFromLastEvent(final Saga saga, final List<SagaEvent> eventStates, final T t) throws InterruptedException, TimeoutException, IOException {
    val sagaEventOptional = this.findTheLastEventOccurred(eventStates);
    if (sagaEventOptional.isPresent()) {
      val sagaEvent = sagaEventOptional.get();
      log.trace(sagaEventOptional.toString());
      final EventType currentEvent = EventType.valueOf(sagaEvent.getSagaEventState());
      final EventOutcome eventOutcome = EventOutcome.valueOf(sagaEvent.getSagaEventOutcome());
      final Event event = Event.builder()
          .eventOutcome(eventOutcome)
          .eventType(currentEvent)
          .eventPayload(sagaEvent.getSagaEventResponse())
          .build();
      final Optional<SagaEventState<T>> sagaEventState = this.findNextSagaEventState(currentEvent, eventOutcome, t);
      if (sagaEventState.isPresent()) {
        log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString(), saga.getSagaId());
        this.invokeNextEvent(event, saga, t, sagaEventState.get());
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
  private void replayFromBeginning(final Saga saga, final T t) throws InterruptedException, TimeoutException, IOException {
    final Event event = Event.builder()
        .eventOutcome(INITIATE_SUCCESS)
        .eventType(INITIATED)
        .build();
    final Optional<SagaEventState<T>> sagaEventState = this.findNextSagaEventState(INITIATED, INITIATE_SUCCESS, t);
    if (sagaEventState.isPresent()) {
      log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString(), saga.getSagaId());
      this.invokeNextEvent(event, saga, t, sagaEventState.get());
    }
  }

  /**
   * this method is called if there is a new message on this specific topic which this service is listening.
   *
   * @param event the event
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Override
  @Async("subscriberExecutor")
  @Transactional
  public void handleEvent(@NotNull final Event event) throws InterruptedException, IOException, TimeoutException {
    log.info("executing saga event {}", event);
    if (this.sagaEventExecutionNotRequired(event)) {
      log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
      return;
    }
    this.broadcastSagaInitiatedMessage(event);

    final var sagaOptional = this.getSagaService().findSagaById(event.getSagaId()); // system expects a saga record to be present here.
    if (sagaOptional.isPresent()) {
      val saga = sagaOptional.get();
      if (!COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())) {//possible duplicate message or force stop scenario check
        final T sagaData = JsonUtil.getJsonObjectFromString(this.clazz, saga.getPayload());
        final var sagaEventState = this.findNextSagaEventState(event.getEventType(), event.getEventOutcome(), sagaData);
        log.trace("found next event as {}", sagaEventState);
        if (sagaEventState.isPresent()) {
          this.process(event, saga, sagaData, sagaEventState.get());
        } else {
          log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
        }
      } else {
        log.info("got message to process saga for saga ID :: {} but saga is already :: {}", saga.getSagaId(), saga.getStatus());
      }
    } else {
      log.error("Saga process without DB record is not expected. {}", event);
    }
  }

  /**
   * Start to execute saga
   *
   * @param payload                  the event payload
   * @param penRequestBatchStudentID the pen request batch student id
   * @param penRequestBatchID        the pen request batch id
   * @param userName                 the user who created the saga
   * @return saga record
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  @Override
  @Transactional
  public Saga startSaga(@NotNull final String payload, final UUID penRequestBatchStudentID, final UUID penRequestBatchID, final String userName) throws InterruptedException, TimeoutException, IOException {
    final var saga = this.sagaService.createSagaRecordInDB(this.sagaName, userName, payload, penRequestBatchStudentID, penRequestBatchID);
    this.handleEvent(Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .sagaId(saga.getSagaId())
        .eventPayload(payload)
        .build());
    return saga;
  }

  /**
   * Start to execute sagas
   *
   * @param payload                  the event payload
   * @param penRequestBatchIDs       the pen request batch ids
   * @param userName                 the user who created the saga
   * @return saga record
   * @throws IOException          the io exception
   */
  @Transactional
  public List<Saga> saveMultipleSagas(@NotNull String payload, List<UUID> penRequestBatchIDs, String userName) throws IOException {
    var sagas = getSagaService().createMultipleBatchSagaRecordsInDB(getSagaName(), userName, payload, penRequestBatchIDs);
    multipleSagaExecutor.execute(() -> {

    });
    return sagas;
  }

  @Async("subscriberExecutor")
  @Transactional
  public void startMultipleSagas(List<Saga> sagas) {
    for(Saga saga : sagas) {
      try {
        handleEvent(Event.builder()
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(saga.getSagaId())
                .eventPayload(saga.getPayload())
                .build());
      } catch (InterruptedException e) {
        log.error("There was an unexpected exception attempting to start multiple sagas", e);
        Thread.currentThread().interrupt();
      } catch (IOException | TimeoutException e) {
        log.error("There was an unexpected exception attempting to start multiple sagas", e);
      }
    }
  }

  /**
   * DONT DO ANYTHING the message was broad-casted for the frontend listeners, that a saga process has initiated, completed.
   *
   * @param event the event object received from queue.
   * @return true if this message need not be processed further.
   */
  private boolean sagaEventExecutionNotRequired(@NotNull final Event event) {
    return (event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS && SELF.equalsIgnoreCase(event.getReplyTo()))
        || event.getEventType() == MARK_SAGA_COMPLETE && event.getEventOutcome() == SAGA_COMPLETED;
  }

  /**
   * Broadcast the saga initiated message
   *
   * @param event the event object
   */
  private void broadcastSagaInitiatedMessage(@NotNull final Event event) {
    // !SELF.equalsIgnoreCase(event.getReplyTo()):- this check makes sure it is not broadcast-ed infinitely.
    if (this.shouldSendNotificationEvent && event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS
        && !SELF.equalsIgnoreCase(event.getReplyTo())) {
      final var notificationEvent = new NotificationEvent();
      BeanUtils.copyProperties(event, notificationEvent);
      notificationEvent.setSagaStatus(INITIATED.toString());
      notificationEvent.setReplyTo(SELF);
      notificationEvent.setSagaName(this.getSagaName());
      this.postMessageToTopic(this.getTopicToSubscribe(), notificationEvent);
    }
  }

  /**
   * this method finds the next event that needs to be executed.
   *
   * @param currentEvent current event
   * @param eventOutcome event outcome.
   * @return {@link Optional<SagaEventState>}
   */
  protected Optional<SagaEventState<T>> findNextSagaEventState(final EventType currentEvent, final EventOutcome eventOutcome, final T sagaData) {
    val sagaEventStates = this.nextStepsToExecute.get(currentEvent);
    return sagaEventStates == null ? Optional.empty() : sagaEventStates.stream().filter(el ->
        el.getCurrentEventOutcome() == eventOutcome && el.nextStepPredicate.test(sagaData)
    ).findFirst();
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
  protected void process(@NotNull final Event event, final Saga saga, final T sagaData, final SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    if (!saga.getSagaState().equalsIgnoreCase(COMPLETED.toString())
        && this.isNotProcessedEvent(event.getEventType(), saga, this.nextStepsToExecute.keySet())) {
      log.info(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.getNextEventType(), event.toString(), saga.getSagaId());
      this.invokeNextEvent(event, saga, sagaData, sagaEventState);
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
  protected void invokeNextEvent(final Event event, final Saga saga, final T sagaData, final SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    final SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }

  /**
   * Populate steps to execute map.
   */
  public abstract void populateStepsToExecuteMap();

}
