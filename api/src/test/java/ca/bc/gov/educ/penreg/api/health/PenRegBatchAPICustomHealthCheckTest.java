package ca.bc.gov.educ.penreg.api.health;

import ca.bc.gov.educ.penreg.api.BaseTest;
import io.nats.client.Connection;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class PenRegBatchAPICustomHealthCheckTest extends BaseTest {

  @Autowired
  Connection natsConnection;

  @Autowired
  private PenRegBatchAPICustomHealthCheck penRegBatchAPICustomHealthCheck;

  @Test
  public void testGetHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true)).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true).getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testGetHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true)).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.getHealth(true).getStatus()).isEqualTo(Status.UP);
  }


  @Test
  public void testHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    assertThat(this.penRegBatchAPICustomHealthCheck.health()).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    assertThat(this.penRegBatchAPICustomHealthCheck.health()).isNotNull();
    assertThat(this.penRegBatchAPICustomHealthCheck.health().getStatus()).isEqualTo(Status.UP);
  }
}
