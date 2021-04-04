package ca.bc.gov.educ.penreg.api;

import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.support.TestRedisConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestRedisConfiguration.class, PenRegBatchApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseTest {
  @Autowired
  private PenRequestBatchUtils penRequestBatchUtils;

  @Before
  public void resetState() {
    this.penRequestBatchUtils.cleanDB();
  }
}
