package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.mappers.v1.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.repository.PenCoordinatorRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class NotificationServiceTest {

  @Autowired
  NotificationService notificationService;

  @Autowired
  PenCoordinatorRepository coordinatorRepository;
  @Autowired
  PenCoordinatorService service;

  @Autowired
  MessagePublisher messagePublisher;

  @Before
  public void setUp() throws Exception {
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.coordinatorRepository.saveAll(structs.stream().map(PenCoordinatorMapper.mapper::toModel).collect(Collectors.toList()));
  }

  @Test
  public void notifySchoolForLoadFailed_givenMinCodeAndPenCoordinatorPresent_shouldSendMessageToNotifySchool() throws ExecutionException, InterruptedException {
    this.service.init();
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
      public byte[] getData() {
        return message.getBytes(StandardCharsets.UTF_8);
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
    });
  }
}