package ca.bc.gov.educ.penreg.api.health;

import io.nats.client.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisCluster;
import org.redisson.api.redisnode.RedisClusterMaster;
import org.redisson.api.redisnode.RedisClusterSlave;
import org.redisson.api.redisnode.RedisNodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class PenRegBatchAPICustomHealthCheckTest {

  @Autowired
  Connection natsConnection;
  @MockBean
  RedissonClient redissonClient;
  @Autowired
  private PenRegBatchAPICustomHealthCheck penRegBatchAPICustomHealthCheck;

  @Before
  public void setup() {
    Mockito.reset(redissonClient);
  }

  @Test
  public void testGetHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    when(redissonClient.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(null);
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true)).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true).getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testGetHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    when(redissonClient.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(this.getRedisClusterMock(true));
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true)).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true).getStatus()).isEqualTo(Status.UP);
  }


  @Test
  public void testHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    when(redissonClient.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(this.getRedisClusterMock(false));
    assertThat(this.penRegBatchAPICustomHealthCheck.health()).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    when(redissonClient.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(this.getRedisClusterMock(true));
    assertThat(this.penRegBatchAPICustomHealthCheck.health()).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.health().getStatus()).isEqualTo(Status.UP);
  }

  private RedisCluster getRedisClusterMock(boolean pingAll) {
    return new RedisCluster() {
      @Override
      public Collection<RedisClusterMaster> getMasters() {
        return Collections.emptyList();
      }

      @Override
      public RedisClusterMaster getMaster(String address) {
        return null;
      }

      @Override
      public Collection<RedisClusterSlave> getSlaves() {
        return null;
      }

      @Override
      public RedisClusterSlave getSlave(String address) {
        return null;
      }

      @Override
      public boolean pingAll() {
        return pingAll;
      }

      @Override
      public boolean pingAll(long timeout, TimeUnit timeUnit) {
        return false;
      }
    };
  }
}
