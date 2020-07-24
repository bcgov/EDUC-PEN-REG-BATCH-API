package ca.bc.gov.educ.penreg.api;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The Pen reg batch api application.
 *
 * @author OM
 */
@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "1s")
@EnableRetry
public class PenRegBatchApiApplication {

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(PenRegBatchApiApplication.class, args);
  }


  /**
   * The type Web security configuration.
   * Add security exceptions for swagger UI and prometheus.
   */
  @Configuration
  static
  class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) {
      web.ignoring().antMatchers("/v3/api-docs/**",
          "/actuator/health", "/actuator/prometheus",
          "/swagger-ui/**", "/health");
    }
  }

  /**
   * Lock provider For distributed lock, to avoid multiple pods executing the same scheduled task.
   *
   * @param jdbcTemplate       the jdbc template
   * @param transactionManager the transaction manager
   * @return the lock provider
   */
  @Bean
  public LockProvider lockProvider(@Autowired JdbcTemplate jdbcTemplate, @Autowired PlatformTransactionManager transactionManager) {
    return new JdbcTemplateLockProvider(jdbcTemplate, transactionManager, "PEN_REG_BATCH_SHEDLOCK");
  }
}
