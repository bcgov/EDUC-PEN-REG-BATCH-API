package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.properties.NotificationProperties;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.struct.v1.notification.PenRequestBatchSchoolErrorNotificationEntity;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC;

/**
 * This class is responsible for different type of notification to be sent out from batch api.
 */
@Service
@Slf4j
public class NotificationService {

  private final MessagePublisher messagePublisher;
  private final NotificationProperties notificationProperties;

  public NotificationService(final MessagePublisher messagePublisher, final NotificationProperties notificationProperties) {
    this.messagePublisher = messagePublisher;
    this.notificationProperties = notificationProperties;
  }

  public CompletableFuture<Boolean> notifySchoolForLoadFailed(final String guid, final String fileName, final String submissionNumber, final String reason, final String toEmail) {
    try {
      val localDateTime = LocalDateTime.now();
      val prbErrorNotification = new PenRequestBatchSchoolErrorNotificationEntity();
      prbErrorNotification.setFailReason(reason);
      prbErrorNotification.setDateTime(localDateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).concat(String.valueOf(localDateTime.getDayOfMonth())).concat(",").concat(String.valueOf(localDateTime.getYear())));
      prbErrorNotification.setFileName(fileName);
      prbErrorNotification.setSubmissionNumber(submissionNumber);
      prbErrorNotification.setToEmail(toEmail);
      prbErrorNotification.setFromEmail(this.notificationProperties.getFromEmail());
      final Event event = Event.builder()
          .eventType(EventType.PEN_REQUEST_BATCH_NOTIFY_SCHOOL_FILE_FORMAT_ERROR)
          .eventPayload(JsonUtil.getJsonStringFromObject(prbErrorNotification))
          .sagaId(UUID.fromString(guid))
          .build();
      return this.messagePublisher.requestMessage(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), JsonUtil.getJsonStringFromObject(event).getBytes(StandardCharsets.UTF_8)).thenApplyAsync(result -> result.getData() != null && result.getData().length > 0);
    } catch (final Exception e) {
      log.error("Exception while sending message for guid :: {}", guid, e);
      return CompletableFuture.completedFuture(false);
    }
  }

}
