package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC;

/**
 * The type Pen reg batch student records processor.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class PenRegBatchStudentRecordsProcessor implements Closeable {
  /**
   * The Executor service.
   */
  private final ExecutorService executorService = Executors.newWorkStealingPool(2);
  /**
   * The Message publisher.
   */
  private final MessagePublisher messagePublisher;


  /**
   * Instantiates a new Pen reg batch student records processor.
   *
   * @param messagePublisher the message publisher
   */
  @Autowired
  public PenRegBatchStudentRecordsProcessor(MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  /**
   * Publish unprocessed student records for processing.
   * this will publish messages to the topic which this api is listening to so that load is balanced as across pods
   * as api is in queue group durable subscription.
   *
   * @param batchStudentSagaDataSet the student entities
   */
  public void publishUnprocessedStudentRecordsForProcessing(final Set<PenRequestBatchStudentSagaData> batchStudentSagaDataSet) {
    executorService.execute(() -> batchStudentSagaDataSet.forEach(sendIndividualStudentAsMessageToTopic()));
  }

  /**
   * Send individual student as message to topic consumer.
   *
   * @return the consumer
   */
  private Consumer<PenRequestBatchStudentSagaData> sendIndividualStudentAsMessageToTopic() {
    return penRequestBatchStudentSagaData -> {
      var eventPayload = JsonUtil.getJsonString(penRequestBatchStudentSagaData);
      if (eventPayload.isPresent()) {
        Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).build();
        var eventString = JsonUtil.getJsonString(event);
        if (eventString.isPresent()) {
          messagePublisher.dispatchMessage(PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString(), eventString.get().getBytes());
        } else {
          log.error("Event Sting is empty, skipping the publish to topic :: {}", penRequestBatchStudentSagaData);
        }
      } else {
        log.error("Event payload is empty, skipping the publish to topic :: {}", penRequestBatchStudentSagaData);
      }
    };
  }

  /**
   * Close.
   */
  @Override
  public void close() {
    if (!executorService.isShutdown()) {
      executorService.shutdown();
    }
  }
}
