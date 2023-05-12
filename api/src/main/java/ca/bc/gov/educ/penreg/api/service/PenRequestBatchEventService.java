package ca.bc.gov.educ.penreg.api.service;


import static ca.bc.gov.educ.penreg.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.ARCHIVED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.REARCHIVED;
import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEvent;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchEventRepository;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchive;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PenRequestBatchEventService {

  public static final String NO_RECORD_SAGA_ID_EVENT_TYPE = "no record found for the saga id and event type combination, processing.";
  public static final String RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE = "record found for the saga id and event type combination, might be a duplicate or replay," +
      " just updating the db status so that it will be polled and sent back again.";
  public static final String EVENT_PAYLOAD = "event is :: {}";
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  private static final PenRequestBatchStudentMapper prbStudentMapper = PenRequestBatchStudentMapper.mapper;
  private static final PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
  @Getter(PRIVATE)
  private final PenRequestBatchEventRepository penRequestBatchEventRepository;

  @Getter(PRIVATE)
  private final PenRequestBatchStudentService prbStudentService;

  @Getter(PRIVATE)
  final PenRequestBatchService penRequestBatchService;

  @Getter(PRIVATE)
  final ResponseFileGeneratorService responseFileGeneratorService;

  @Autowired
  public PenRequestBatchEventService(final PenRequestBatchEventRepository penRequestBatchEventRepository,
                                     final PenRequestBatchStudentService prbStudentService,
                                     final PenRequestBatchService penRequestBatchService,
                                     final ResponseFileGeneratorService responseFileGeneratorService) {
    this.prbStudentService = prbStudentService;
    this.penRequestBatchEventRepository = penRequestBatchEventRepository;
    this.penRequestBatchService = penRequestBatchService;
    this.responseFileGeneratorService = responseFileGeneratorService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PenRequestBatchEvent updatePenRequestBatchStudent(final Event event) throws JsonProcessingException {
    val penRequestBatchEventOptional = this.getPenRequestBatchEventRepository().findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    final PenRequestBatchEvent penRequestBatchEvent;
    if (penRequestBatchEventOptional.isEmpty()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      final var prbStudent = JsonUtil.getJsonObjectFromString(PenRequestBatchStudent.class, event.getEventPayload());
      final PenRequestBatchStudentEntity entity = prbStudentMapper.toModel(prbStudent);
      this.populateAuditColumnsForUpdateStudent(entity);

      try {
        this.prbStudentService.updateStudent(entity, UUID.fromString(prbStudent.getPenRequestBatchID()), UUID.fromString(prbStudent.getPenRequestBatchStudentID()));
        event.setEventPayload(JsonUtil.getJsonStringFromObject(prbStudentMapper.toStructure(entity)));// need to convert to structure MANDATORY otherwise jackson will break.
        event.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_STUDENT_UPDATED);
      } catch (final EntityNotFoundException ex) {
        log.error("PenRequestBatchStudent not found while trying to update it", ex);
        event.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_STUDENT_NOT_FOUND);
      }
      penRequestBatchEvent = this.createPenRequestBatchEventRecord(event);
    } else {
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      penRequestBatchEvent = penRequestBatchEventOptional.get();
      penRequestBatchEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
    }

    this.getPenRequestBatchEventRepository().save(penRequestBatchEvent);
    return penRequestBatchEvent;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PenRequestBatchEvent archivePenRequestBatch(final Event event) throws JsonProcessingException {
    val penRequestBatchEventOptional = this.getPenRequestBatchEventRepository().findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    final PenRequestBatchEvent penRequestBatchEvent;
    if (penRequestBatchEventOptional.isEmpty()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      final var penRequestBatchArchive = JsonUtil.getJsonObjectFromString(PenRequestBatchArchive.class, event.getEventPayload());
      var penRequestBatchEntityOptional = getPenRequestBatchService().findById(penRequestBatchArchive.getPenRequestBatchID());

      if(penRequestBatchEntityOptional.isPresent()) {
        PenRequestBatchEntity penRequestBatch = penRequestBatchEntityOptional.get();
        if (StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED.getCode())
          || StringUtils.equals(penRequestBatch.getPenRequestBatchStatusCode(), PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode())) {
          penRequestBatch.setPenRequestBatchStatusCode(REARCHIVED.getCode());
        } else {
          penRequestBatch.setPenRequestBatchStatusCode(ARCHIVED.getCode());
        }
        if (penRequestBatchArchive.getUpdateUser() == null) {
          penRequestBatch.setUpdateUser(PEN_REQUEST_BATCH_API);
        } else {
          penRequestBatch.setUpdateUser(penRequestBatchArchive.getUpdateUser());
        }
        penRequestBatch.setProcessDate(LocalDateTime.now());
        try {
          getPenRequestBatchService().updatePenRequestBatch(penRequestBatch, penRequestBatch.getPenRequestBatchID());
          event.setEventPayload(JsonUtil.getJsonStringFromObject(batchMapper.toStructure(penRequestBatch)));
          event.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_UPDATED);
        } catch (EntityNotFoundException ex) {
          log.error("PenRequestBatch not found while trying to update it. This should not happen :: ", ex);
          event.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
        }
      } else {
        log.error("PenRequestBatch not found while trying to update it. This should not happen :: " + penRequestBatchArchive.getPenRequestBatchID());
        event.setEventOutcome(EventOutcome.PEN_REQUEST_BATCH_NOT_FOUND);
      }
      penRequestBatchEvent = this.createPenRequestBatchEventRecord(event);

    } else {
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      penRequestBatchEvent = penRequestBatchEventOptional.get();
      penRequestBatchEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
    }

    this.getPenRequestBatchEventRepository().save(penRequestBatchEvent);
    return penRequestBatchEvent;
  }

  private PenRequestBatchEvent createPenRequestBatchEventRecord(final Event event) {
    return PenRequestBatchEvent.builder()
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .createUser(event.getEventType().toString()) //need to discuss what to put here.
        .updateUser(event.getEventType().toString())
        .eventPayload(event.getEventPayload())
        .eventType(event.getEventType().toString())
        .sagaId(event.getSagaId())
        .eventStatus(MESSAGE_PUBLISHED.toString())
        .eventOutcome(event.getEventOutcome().toString())
        .replyChannel(event.getReplyTo())
        .build();
  }

  /**
   * Populate audit columns for student.
   *
   * @param model the model
   */
  private void populateAuditColumnsForUpdateStudent(final PenRequestBatchStudentEntity model) {
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setUpdateDate(LocalDateTime.now());
  }
}
