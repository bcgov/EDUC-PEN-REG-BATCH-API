package ca.bc.gov.educ.penreg.api.support;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class TestRedisConfiguration {

  private final RedisServer redisServer;

  public TestRedisConfiguration() {
    this.redisServer = RedisServer.builder().setting("maxmemory 128M").port(6370).build();
  }

  @PostConstruct
  public void postConstruct() {
    this.redisServer.start();
  }

  @PreDestroy
  public void preDestroy() {
    this.redisServer.stop();
  }
}
