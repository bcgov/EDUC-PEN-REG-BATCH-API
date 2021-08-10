package ca.bc.gov.educ.penreg.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@TestConfiguration
public class TestRedisConfiguration {

  private final RedisServer redisServer;

  public TestRedisConfiguration() {
    this.redisServer = RedisServer.builder().setting("maxheap 15M").port(6370).build();
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
