package ca.bc.gov.educ.penreg.api.health;

import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisCluster;
import org.redisson.api.redisnode.RedisNodes;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PenRegBatchAPICustomHealthCheck implements HealthIndicator {
  private final Connection natsConnection;
  private final RedissonClient redissonClient;

  public PenRegBatchAPICustomHealthCheck(final Connection natsConnection, final RedissonClient redissonClient) {
    this.natsConnection = natsConnection;
    this.redissonClient = redissonClient;
  }

  @Override
  public Health getHealth(final boolean includeDetails) {
    return this.healthCheck();
  }


  @Override
  public Health health() {
    return this.healthCheck();
  }

  private Health healthCheck() {
    final boolean isRedisDown = this.isRedisDown();

    if (this.natsConnection.getStatus() == Connection.Status.CLOSED) {
      log.warn("Health Check failed for NATS");
      return Health.down().withDetail("NATS", " Connection is Closed.").build();
    } else if (isRedisDown) {
      log.warn("Health Check failed for REDIS");
      return Health.down().withDetail("REDIS", "Connection is not stable across cluster.").build();
    }
    return Health.up().build();
  }

  private boolean isRedisDown() {
    boolean isRedisDown = false;
    val redisClusterNodes = this.redissonClient.getRedisNodes(RedisNodes.CLUSTER);
    if (redisClusterNodes != null && redisClusterNodes.getMasters() != null && redisClusterNodes.getSlaves() != null) {
      final int masterPingsFailed = this.getMasterPingsFailed(redisClusterNodes);
      final int slavePingsFailed = this.getSlavePingsFailed(redisClusterNodes);
      log.debug("Redis masters size :: {}, slaves size :: {}, failed pings for server masters :: {}, slaves :: {}", redisClusterNodes.getMasters().size(), redisClusterNodes.getSlaves().size(), masterPingsFailed, slavePingsFailed);
      if (masterPingsFailed == redisClusterNodes.getMasters().size() || slavePingsFailed == redisClusterNodes.getSlaves().size()) {
        isRedisDown = true;
      }
    } else {
      log.warn("Redis cluster is null");
    }
    return isRedisDown;
  }

  private int getSlavePingsFailed(final RedisCluster redisClusterNodes) {
    int slavePingsFailed = 0;
    for (val slave : redisClusterNodes.getSlaves()) {
      try {
        val pongResult = slave.ping(2, TimeUnit.SECONDS);
        if (!pongResult) {
          slavePingsFailed++;
        }
      } catch (final Exception e) {
        slavePingsFailed++;
      }
    }
    return slavePingsFailed;
  }

  private int getMasterPingsFailed(final RedisCluster redisClusterNodes) {
    int masterPingsFailed = 0;
    for (val master : redisClusterNodes.getMasters()) {
      try {
        val pongResult = master.ping(2, TimeUnit.SECONDS);
        if (!pongResult) {
          masterPingsFailed++;
        }
      } catch (final Exception e) {
        masterPingsFailed++;
      }
    }
    return masterPingsFailed;
  }
}
