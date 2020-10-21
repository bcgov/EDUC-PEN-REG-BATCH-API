package ca.bc.gov.educ.penreg.api.config;

import ca.bc.gov.educ.penreg.api.util.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfiguration {
  /**
   * Thread pool task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "subscriberExecutor")
  public Executor threadPoolTaskExecutor() {
    ThreadFactory namedThreadFactory =
      new ThreadFactoryBuilder().withNameFormat("message-subscriber-%d").get();
    return Executors.newFixedThreadPool(10, namedThreadFactory);
  }

  /**
   * Controller task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "taskExecutor")
  public Executor controllerTaskExecutor() {
    ThreadFactory namedThreadFactory =
      new ThreadFactoryBuilder().withNameFormat("async-executor-%d").get();
    return Executors.newFixedThreadPool(8, namedThreadFactory);
  }
}
