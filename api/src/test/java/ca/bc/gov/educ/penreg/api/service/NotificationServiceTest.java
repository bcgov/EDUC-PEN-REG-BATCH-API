package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.Status;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NotificationServiceTest extends BasePenRegAPITest {

  @Autowired
  NotificationService notificationService;

  @Autowired
  StudentRegistrationContactService service;

  @Autowired
  MessagePublisher messagePublisher;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void notifySchoolForLoadFailed_givenMinCodeAndStudentRegistrationSchoolContactEmailPresent_shouldSendMessageToNotifySchool() throws ExecutionException, InterruptedException {
    when(this.messagePublisher.requestMessage(any(), any())).thenReturn(this.returnMessage("success"));
    this.notificationService.notifySchoolForLoadFailed(UUID.randomUUID().toString(), "PEN_TEST", "123456789", "failed", List.of("test@test.ca")).get();
    verify(this.messagePublisher, atLeastOnce()).requestMessage(any(), any());
  }

  private CompletableFuture<Message> returnMessage(final String message) {
    return CompletableFuture.completedFuture(new Message() {
      @Override
      public String getSubject() {
        return null;
      }

      @Override
      public String getReplyTo() {
        return null;
      }

      @Override
      public boolean hasHeaders() {
        return false;
      }

      @Override
      public Headers getHeaders() {
        return null;
      }

      @Override
      public boolean isStatusMessage() {
        return false;
      }

      @Override
      public Status getStatus() {
        return null;
      }

      @Override
      public byte[] getData() {
        return message.getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public boolean isUtf8mode() {
        return false;
      }

      @Override
      public Subscription getSubscription() {
        return null;
      }

      @Override
      public String getSID() {
        return null;
      }

      @Override
      public Connection getConnection() {
        return null;
      }

      @Override
      public NatsJetStreamMetaData metaData() {
        return null;
      }

      @Override
      public void ack() {

      }

      @Override
      public void ackSync(final Duration duration) throws TimeoutException, InterruptedException {

      }

      @Override
      public void nak() {

      }

      @Override
      public void term() {

      }

      @Override
      public void inProgress() {

      }

      @Override
      public boolean isJetStream() {
        return false;
      }
    });
  }
}
