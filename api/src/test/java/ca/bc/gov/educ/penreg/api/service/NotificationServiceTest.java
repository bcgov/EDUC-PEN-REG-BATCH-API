package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.Status;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
  }

  @Test
  public void notifySchoolForLoadFailed_givenMinCodeAndPenCoordinatorPresent_shouldSendMessageToNotifySchool() throws ExecutionException, InterruptedException {
    when(this.messagePublisher.requestMessage(any(), any())).thenReturn(this.returnMessage("success"));
    this.notificationService.notifySchoolForLoadFailed(UUID.randomUUID().toString(), "PEN_TEST", "123456789", "failed", "test@test.ca").get();
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
