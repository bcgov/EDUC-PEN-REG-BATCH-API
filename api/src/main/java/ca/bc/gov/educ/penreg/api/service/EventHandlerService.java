package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.model.StudentEntity;
import ca.bc.gov.educ.penreg.api.model.StudentEvent;
import ca.bc.gov.educ.penreg.api.repository.StudentEventRepository;
import ca.bc.gov.educ.penreg.api.repository.StudentRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.constant.EventStatus.DB_COMMITTED;
import static ca.bc.gov.educ.api.student.constant.EventStatus.MESSAGE_PUBLISHED;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventHandlerService {

  public static final String NO_RECORD_SAGA_ID_EVENT_TYPE = "no record found for the saga id and event type combination, processing.";
  public static final String RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE = "record found for the saga id and event type combination, might be a duplicate or replay," +
          " just updating the db status so that it will be polled and sent back again.";
  public static final String PAYLOAD_LOG = "payload is :: {}";
  public static final String EVENT_PAYLOAD = "event is :: {}";
  @Getter(PRIVATE)
  private final StudentRepository studentRepository;
  private static final StudentMapper mapper = StudentMapper.mapper;
  @Getter(PRIVATE)
  private final StudentEventRepository studentEventRepository;

  @Autowired
  public EventHandlerService(final StudentRepository studentRepository, final StudentEventRepository studentEventRepository) {
    this.studentRepository = studentRepository;
    this.studentEventRepository = studentEventRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleEvent(Event event) {
    try {
      switch (event.getEventType()) {
        case STUDENT_EVENT_OUTBOX_PROCESSED:
          log.info("received outbox processed event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleStudentOutboxProcessedEvent(event.getEventPayload());
          break;
        case GET_STUDENT:
          log.info("received get student event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleGetStudentEvent(event);
          break;
        case CREATE_STUDENT:
          log.info("received create student event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleCreateStudentEvent(event);
          break;
        case UPDATE_STUDENT:
          log.info("received update student event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          handleUpdateStudentEvent(event);
          break;
        default:
          log.info("silently ignoring other events.");
          break;
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  private void handleUpdateStudentEvent(Event event) throws JsonProcessingException {
    val studentEventOptional = getStudentEventRepository().findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    StudentEvent studentEvent;
    if (!studentEventOptional.isPresent()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      StudentEntity entity = mapper.toModel(JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload()));
      val optionalStudent = getStudentRepository().findById(entity.getStudentID());
      if (optionalStudent.isPresent()) {
        val studentDBEntity = optionalStudent.get();
        BeanUtils.copyProperties(entity, studentDBEntity);
        studentDBEntity.setUpdateDate(LocalDateTime.now());
        getStudentRepository().save(studentDBEntity);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(studentDBEntity)));// need to convert to structure MANDATORY otherwise jackson will break.
        event.setEventOutcome(EventOutcome.STUDENT_UPDATED);
      } else {
        event.setEventOutcome(EventOutcome.STUDENT_NOT_FOUND);
      }
      studentEvent = createStudentEventRecord(event);
    } else {
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      studentEvent = studentEventOptional.get();
      studentEvent.setEventStatus(DB_COMMITTED.toString());
    }

    getStudentEventRepository().save(studentEvent);
  }

  private void handleCreateStudentEvent(Event event) throws JsonProcessingException {
    val studentEventOptional = getStudentEventRepository().findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    StudentEvent studentEvent;
    if (!studentEventOptional.isPresent()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      StudentEntity entity = mapper.toModel(JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload()));
      val optionalStudent = getStudentRepository().findStudentEntityByPen(entity.getPen());
      if (optionalStudent.isPresent()) {
        event.setEventOutcome(EventOutcome.STUDENT_ALREADY_EXIST);
      } else {
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        getStudentRepository().save(entity);
        event.setEventOutcome(EventOutcome.STUDENT_CREATED);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(entity)));// need to convert to structure MANDATORY otherwise jackson will break.
      }
      studentEvent = createStudentEventRecord(event);
    } else {
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      studentEvent = studentEventOptional.get();
      studentEvent.setEventStatus(DB_COMMITTED.toString());
    }

    getStudentEventRepository().save(studentEvent);
  }

  private void handleStudentOutboxProcessedEvent(String studentEventId) {
    val studentEventFromDB = getStudentEventRepository().findById(UUID.fromString(studentEventId));
    if (studentEventFromDB.isPresent()) {
      val studEvent = studentEventFromDB.get();
      studEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
      getStudentEventRepository().save(studEvent);
    }
  }

  /**
   * Saga should never be null for this type of event.
   * this method expects that the event payload contains a pen number.
   *
   * @param event containing the student PEN.
   */
  public void handleGetStudentEvent(Event event) throws JsonProcessingException {
    val studentEventOptional = getStudentEventRepository().findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    StudentEvent studentEvent;
    if (!studentEventOptional.isPresent()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      val optionalStudentEntity = getStudentRepository().findStudentEntityByPen(event.getEventPayload());
      if (optionalStudentEntity.isPresent()) {
        Student student = mapper.toStructure(optionalStudentEntity.get()); // need to convert to structure MANDATORY otherwise jackson will break.
        event.setEventPayload(JsonUtil.getJsonStringFromObject(student));
        event.setEventOutcome(EventOutcome.STUDENT_FOUND);
      } else {
        event.setEventOutcome(EventOutcome.STUDENT_NOT_FOUND);
      }
      studentEvent = createStudentEventRecord(event);
    } else { // just update the status of the event so that it will be polled and send again to the saga orchestrator.
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      studentEvent = studentEventOptional.get();
      studentEvent.setEventStatus(DB_COMMITTED.toString());
    }
    getStudentEventRepository().save(studentEvent);
  }

  private StudentEvent createStudentEventRecord(Event event) {
    return StudentEvent.builder()
            .createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .createUser(event.getEventType().toString()) //need to discuss what to put here.
            .updateUser(event.getEventType().toString())
            .eventPayload(event.getEventPayload())
            .eventType(event.getEventType().toString())
            .sagaId(event.getSagaId())
            .eventStatus(DB_COMMITTED.toString())
            .eventOutcome(event.getEventOutcome().toString())
            .replyChannel(event.getReplyTo())
            .build();
  }
}
