package ca.bc.gov.educ.penreg.api.config;

import ca.bc.gov.educ.penreg.api.util.ThreadFactoryBuilder;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.concurrent.Executor;

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
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("message-subscriber-%d").get())
      .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  /**
   * Controller task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "taskExecutor")
  public Executor controllerTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
        .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-executor-%d").get())
        .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  /**
   * Controller task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "penWebBlobExtractor")
  public Executor penWebBlobExtractorExecutor() {
    return new EnhancedQueueExecutor.Builder()
        .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("pen-web-blob-processor-%d").get())
        .setCorePoolSize(1).setMaximumPoolSize(1).build();
  }
}
