package ca.bc.gov.educ.penreg.api.messaging;

import ca.bc.gov.educ.penreg.api.constants.SagaTopicsEnum;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import io.nats.streaming.AckHandler;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class MessagePublisherTest {
  /**
   * The constant PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.
   */
  public static final String PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC = SagaTopicsEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC.toString();

  /**
   * The Executor service.
   */
  @Mock
  private ExecutorService executorService;
  /**
   * The Connection.
   */
  @Mock
  private StreamingConnection connection;
  /**
   * The Connection factory.
   */
  @Mock
  private StreamingConnectionFactory connectionFactory;
  /**
   * The Application properties.
   */
  @Autowired
  private ApplicationProperties applicationProperties;
  /**
   * The Message publisher.
   */
  private MessagePublisher messagePublisher;

  /**
   * Sets up.
   *
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   */
  @Before
  public void setUp() throws IOException, InterruptedException {
    initMocks(this);
    messagePublisher = new MessagePublisher(applicationProperties, false);
    messagePublisher.setExecutorService(executorService);
    messagePublisher.setConnectionFactory(connectionFactory);
    when(connectionFactory.createConnection()).thenReturn(connection);
    messagePublisher.connect();
  }

  /**
   * Test message publisher given invalid nats url should thrown exception.
   *
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   */
  @Test(expected = IOException.class)
  public void testMessagePublisher_givenInvalidNatsUrl_shouldThrownException() throws IOException, InterruptedException {
    var messagePublisher = new MessagePublisher(applicationProperties);
  }

  /**
   * Test dispatch message given message should publish.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDispatchMessage_givenMessage_shouldPublish() throws Exception {
    messagePublisher.dispatchMessage(PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC, "Test".getBytes());
    verify(connection, atMostOnce()).publish(eq(PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC), aryEq("Test".getBytes()), any(AckHandler.class));
  }

  /**
   * Test retry publish given message should publish.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRetryPublish_givenMessage_shouldPublish() throws Exception {
    messagePublisher.retryPublish(PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC, "Test".getBytes());
    verify(connection, atMostOnce()).publish(eq(PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC), aryEq("Test".getBytes()), any(AckHandler.class));
  }


  /**
   * Test close should close.
   *
   * @throws Exception the exception
   */
  @Test
  public void testClose_shouldClose() throws Exception {
    messagePublisher.close();
    verify(connection, atMostOnce()).close();
  }

  /**
   * Test close given exception should close.
   *
   * @throws Exception the exception
   */
  @Test
  public void testClose_givenException_shouldClose() throws Exception {
    doThrow(new IOException("Test")).when(connection).close();
    messagePublisher.close();
    verify(connection, atMostOnce()).close();
  }

  /**
   * Test on ack given exception should retry publish.
   *
   * @throws Exception the exception
   */
  @Test
  public void testOnAck_givenException_shouldRetryPublish() throws Exception {
    var ackHandler = messagePublisher.getAckHandler();
    ackHandler.onAck(UUID.randomUUID().toString(), new Exception());
    ackHandler.onAck(UUID.randomUUID().toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC, "Test".getBytes(), null);
    ackHandler.onAck(UUID.randomUUID().toString(), PEN_REQUEST_BATCH_STUDENT_PROCESSING_TOPIC, "Test".getBytes(), new Exception());
    verify(executorService, atMostOnce()).execute(any(Runnable.class));
  }

  /**
   * Test connection lost handler given exception should create connection.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConnectionLostHandler_givenException_shouldCreateConnection() throws Exception {
    when(connectionFactory.createConnection()).thenThrow(new IOException("Test")).thenReturn(connection);
    messagePublisher.connectionLostHandler(connection, new Exception());
    verify(connectionFactory, atLeast(3)).createConnection();
  }

}
