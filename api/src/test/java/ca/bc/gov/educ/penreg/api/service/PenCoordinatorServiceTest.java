package ca.bc.gov.educ.penreg.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;


public class PenCoordinatorServiceTest extends BasePenRegAPITest {

  private final Map<String, PenCoordinator> penCoordinatorMap = new ConcurrentHashMap<>();

  @Autowired
  PenCoordinatorService service;

  @Autowired
  RestUtils restUtils;

  @Before
  public void setup() throws IOException {
    Mockito.reset(restUtils);
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    penCoordinatorMap.putAll(structs.stream().collect(Collectors.toConcurrentMap(key -> String.valueOf(key.getDistrictNumber()).concat(String.valueOf(key.getSchoolNumber())), Function.identity())));
  }

  @Test
  public void testGetPenCoordinator_givenDifferentInputs_shouldProduceOutput() {
    String mincode = "123546789";
    when(this.restUtils.getPenCoordinator(mincode)).thenReturn(Optional.ofNullable(penCoordinatorMap.get(mincode)));
    val data = this.service.getPenCoordinatorByMinCode("123546789");
    assertThat(data).isEmpty();
    when(this.restUtils.getPenCoordinator("19337120")).thenReturn(Optional.ofNullable(penCoordinatorMap.get("19337120")));
    val dataOptional = this.service.getPenCoordinatorByMinCode("19337120");
    assertThat(dataOptional).isPresent();
  }

  @Test
  public void testGetPenCoordinatorEmail_givenDifferentInputs_shouldProduceOutput() {
    when(this.restUtils.getPenCoordinator("19337120")).thenReturn(Optional.ofNullable(penCoordinatorMap.get("19337120")));
    val dataOptional = this.service.getPenCoordinatorEmailByMinCode("19337120");
    assertThat(dataOptional).isPresent();
    assertThat(dataOptional.get()).isEqualTo("jhamberston0@va.gov");
  }

}
