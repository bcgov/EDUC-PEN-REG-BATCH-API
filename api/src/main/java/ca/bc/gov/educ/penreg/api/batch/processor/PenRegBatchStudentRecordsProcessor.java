package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen reg batch student records processor.
 */
@Component
@Slf4j
public class PenRegBatchStudentRecordsProcessor {
  /**
   * The Message publisher.
   */
  private final MessagePublisher messagePublisher;
  /**
   * The pen request batch file service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchFileService penRequestBatchFileService;


  /**
   * Instantiates a new Pen reg batch student records processor.
   *
   * @param messagePublisher           the message publisher
   * @param penRequestBatchFileService the pen request batch file service
   */
  @Autowired
  public PenRegBatchStudentRecordsProcessor(final MessagePublisher messagePublisher, final PenRequestBatchFileService penRequestBatchFileService) {
    this.messagePublisher = messagePublisher;
    this.penRequestBatchFileService = penRequestBatchFileService;
  }

  /**
   * Publish unprocessed student records for processing.
   * this will publish messages to the topic which this api is listening to so that load is balanced as across pods
   * as api is in queue group durable subscription.
   *
   * @param batchStudentSagaDataSet the student entities
   */
  public void publishUnprocessedStudentRecordsForProcessing(final Set<PenRequestBatchStudentSagaData> batchStudentSagaDataSet) {
    batchStudentSagaDataSet.forEach(this.sendIndividualStudentAsMessageToTopic());
  }

  /**
   * Send individual student as message to topic consumer.
   *
   * @return the consumer
   */
  private Consumer<PenRequestBatchStudentSagaData> sendIndividualStudentAsMessageToTopic() {
    return penRequestBatchStudentSagaData -> {
      final var eventPayload = JsonUtil.getJsonString(penRequestBatchStudentSagaData);
      if (eventPayload.isPresent()) {
        final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).build();
        final var eventString = JsonUtil.getJsonString(event);
        if (eventString.isPresent()) {
          this.messagePublisher.dispatchMessage(PEN_REQUEST_BATCH_API_TOPIC.toString(), eventString.get().getBytes());
        } else {
          log.error("Event Sting is empty, skipping the publish to topic :: {}", penRequestBatchStudentSagaData);
        }
      } else {
        log.error("Event payload is empty, skipping the publish to topic :: {}", penRequestBatchStudentSagaData);
      }
    };
  }

  /**
   * Filters repeats for all pen request batches in loaded status.
   *
   * @param penRequestBatchEntities the list of pen request batch entities
   */
  public void checkLoadedStudentRecordsForDuplicatesAndRepeats(final List<PenRequestBatchEntity> penRequestBatchEntities) {
    penRequestBatchEntities.forEach(penRequestBatchEntity -> this.getPenRequestBatchFileService().filterDuplicatesAndRepeatRequests(penRequestBatchEntity.getPenRequestBatchID().toString(), penRequestBatchEntity));
  }

}
