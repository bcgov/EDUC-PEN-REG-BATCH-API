package ca.bc.gov.educ.penreg.api.support;
import ca.bc.gov.educ.penreg.api.messaging.MessagePublisher;
import ca.bc.gov.educ.penreg.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.penreg.api.schedulers.EventTaskScheduler;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * The type Mock configuration.
 */
@Profile("test")
@Configuration
public class MockConfiguration {
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

}