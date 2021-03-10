package ca.bc.gov.educ.penreg.api.support;

import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.messaging.NatsConnection;
import ca.bc.gov.educ.penreg.api.schedulers.EventTaskScheduler;
import io.nats.client.Connection;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The type Mock configuration.
 */
@Profile("testWebclient")
@Configuration
public class MockConfigurationWebClient {
  /**
   * Message publisher message publisher.
   *
   * @return the message publisher
   */
  @Bean
  @Primary
  public MessagePublisher messagePublisher() {
    return Mockito.mock(MessagePublisher.class);
  }

  /**
   * Message subscriber message subscriber.
   *
   * @return the message subscriber
   */
  @Bean
  @Primary
  public MessageSubscriber messageSubscriber() {
    return Mockito.mock(MessageSubscriber.class);
  }

  /**
   * Event task scheduler event task scheduler.
   *
   * @return the event task scheduler
   */
  @Bean
  @Primary
  public EventTaskScheduler eventTaskScheduler() {
    return Mockito.mock(EventTaskScheduler.class);
  }

  @Bean
  @Primary
  public WebClient webClient() {
    return Mockito.mock(WebClient.class);
  }


  @Bean
  @Primary
  public Connection connection() {
    return Mockito.mock(Connection.class);
  }

  @Bean
  @Primary
  public NatsConnection natsConnection() {
    return Mockito.mock(NatsConnection.class);
  }
}
