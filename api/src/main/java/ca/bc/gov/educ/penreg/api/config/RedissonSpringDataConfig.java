package ca.bc.gov.educ.penreg.api.config;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonSpringDataConfig {

  private final ApplicationProperties applicationProperties;

  public RedissonSpringDataConfig(ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Bean
  public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
    return new RedissonConnectionFactory(redisson);
  }

  @Bean(destroyMethod = "shutdown")
  public RedissonClient redisson() {
    RedissonClient redisson;
    if ("local".equals(applicationProperties.getEnvironment())) {
      redisson= Redisson.create();
    } else {
      Config config = new Config();
      config.useClusterServers()
          .addNodeAddress(applicationProperties.getRedisUrl());
      redisson = Redisson.create(config);
    }
    redisson.getConfig().setCodec(StringCodec.INSTANCE);
    return redisson;
  }
}
