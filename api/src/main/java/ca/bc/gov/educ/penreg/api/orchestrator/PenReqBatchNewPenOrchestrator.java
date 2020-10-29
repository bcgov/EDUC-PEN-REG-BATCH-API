package ca.bc.gov.educ.penreg.api.orchestrator;

import ca.bc.gov.educ.penreg.api.constants.TwinReasonCodes;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.model.SagaEvent;
import ca.bc.gov.educ.penreg.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static ca.bc.gov.educ.penreg.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.USR_NEW_PEN;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.*;
import static java.util.stream.Collectors.toList;

/**
 * The type Pen req batch student orchestrator.
 */
@Component
@Slf4j
public class PenReqBatchNewPenOrchestrator extends BaseOrchestrator<PenRequestBatchNewPenSagaData> {
  /**
   * The constant studentMapper.
   */
  private static final StudentMapper studentMapper = StudentMapper.mapper;
  /**
   * The constant penRequestBatchStudentMapper.
   */
  private static final PenRequestBatchStudentMapper penRequestBatchStudentMapper = PenRequestBatchStudentMapper.mapper;

  private PenReqBatchNewPenOrchestrator orchestrator;

  /**
   * Instantiates a new Pen req batch student orchestrator.
   *
   * @param sagaService                               the saga service
   * @param messagePublisher                          the message publisher
   * @param messageSubscriber                         the message subscriber
   * @param taskSchedulerService                      the task scheduler service
   */
  @Autowired
  public PenReqBatchNewPenOrchestrator(SagaService sagaService, MessagePublisher messagePublisher,
                                       MessageSubscriber messageSubscriber, EventTaskSchedulerAsyncService taskSchedulerService) {
    super(sagaService, messagePublisher, messageSubscriber, taskSchedulerService, PenRequestBatchNewPenSagaData.class,
      PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString());
  }

  @Autowired
  public void setPenReqBatchNewPenOrchestrator(final PenReqBatchNewPenOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostConstruct
  private void registerToServices() {
    getMessageSubscriber().subscribe(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_TOPIC.toString(), orchestrator);
    getTaskSchedulerService().registerSagaOrchestrators(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), orchestrator);
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .begin(GET_NEXT_PEN_NUMBER, this::getNextPenNumber)
        .step(GET_NEXT_PEN_NUMBER, NEXT_PEN_NUMBER_RETRIEVED, CREATE_STUDENT, this::createStudent)
        .step(CREATE_STUDENT, STUDENT_ALREADY_EXIST, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .step(CREATE_STUDENT, STUDENT_CREATED, UPDATE_PEN_REQUEST_BATCH_STUDENT, this::updatePenRequestBatchStudent)
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_UPDATED)
        .or()
        .end(UPDATE_PEN_REQUEST_BATCH_STUDENT, PEN_REQUEST_BATCH_STUDENT_NOT_FOUND, this::logPenRequestBatchStudentNotFound);
  }

  /**
   * Get the next PEN number.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchNewPenSagaData the pen request batch student saga data
   */
  public void getNextPenNumber(Event event, Saga saga, PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData) {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_NEXT_PEN_NUMBER.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    var transactionID = saga.getSagaId().toString();
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_NEXT_PEN_NUMBER)
      .replyTo(getTopicToSubscribe())
      .eventPayload(transactionID)
      .build();
    postMessageToTopic(PEN_SERVICES_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_SERVICES_API_TOPIC for GET_NEXT_PEN_NUMBER Event. :: {}", saga.getSagaId());
  }

  /**
   * Create student record.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchNewPenSagaData the pen request batch student saga data
   */
  public void createStudent(Event event, Saga saga, PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData) throws JsonProcessingException {
    var pen = event.getEventPayload();

    var student = studentMapper.toStudent(penRequestBatchNewPenSagaData);
    student.setPen(pen);
    student.setDemogCode("A");
    student.setStudentTwinAssociations(penRequestBatchNewPenSagaData.getTwinStudentIDs().stream().map(studentID ->
      new StudentTwinAssociation(studentID, TwinReasonCodes.PENCREATE.getCode())).collect(toList()));

    penRequestBatchNewPenSagaData.setAssignedPEN(pen);
    saga.setSagaState(CREATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchNewPenSagaData));
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(CREATE_STUDENT)
      .replyTo(getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(student))
      .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event. :: {}", saga.getSagaId());
  }

  /**
   * Update PEN Request Batch record and PRB Student record.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchNewPenSagaData the pen request batch student saga data
   */
  public void updatePenRequestBatchStudent(Event event, Saga saga, PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData) throws JsonProcessingException {
    var student = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());

    var prbStudent = penRequestBatchStudentMapper.toPrbStudent(penRequestBatchNewPenSagaData);
    prbStudent.setPenRequestBatchStudentStatusCode(USR_NEW_PEN.getCode());
    prbStudent.setStudentID(student.getStudentID());
    prbStudent.setAssignedPEN(penRequestBatchNewPenSagaData.getAssignedPEN());

    penRequestBatchNewPenSagaData.setStudentID(student.getStudentID());
    saga.setSagaState(UPDATE_PEN_REQUEST_BATCH_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestBatchNewPenSagaData));
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
   * Update PEN Request Batch record and PRB Student record.
   *
   * @param event                          the event
   * @param saga                           the saga
   * @param penRequestBatchNewPenSagaData the pen request batch student saga data
   */
  public void logPenRequestBatchStudentNotFound(Event event, Saga saga, PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData)  {
    log.error("Pen request batch student record was not found. This should not happen. Please check the batch api. :: {}", saga.getSagaId());
  }

}
