package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BaseTest;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenCoordinatorMapper;
import ca.bc.gov.educ.penreg.api.model.v1.Mincode;
import ca.bc.gov.educ.penreg.api.repository.PenCoordinatorRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class PenCoordinatorServiceTest extends BaseTest {

  @Autowired
  PenCoordinatorRepository coordinatorRepository;

  @Autowired
  PenCoordinatorService service;

  @Before
  public void setup() throws IOException {
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-pen-coordinator.json")).getFile());
    final List<PenCoordinator> structs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.coordinatorRepository.saveAll(structs.stream().map(PenCoordinatorMapper.mapper::toModel).collect(Collectors.toList()));
    this.service.setPenCoordinatorMap(this.coordinatorRepository.findAll().stream()
            .map(ca.bc.gov.educ.penreg.api.batch.mappers.PenCoordinatorMapper.mapper::toTrimmedPenCoordinator)
            .collect(Collectors.toConcurrentMap(ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator::getMincode, Function.identity())));
  }

  @Test
  public void testGetPenCoordinator_givenDifferentInputs_shouldProduceOutput() {
    this.service.init();
    val data = this.service.getPenCoordinatorByMinCode("123546789");
    assertThat(data).isEmpty();
    val dataOptional = this.service.getPenCoordinatorByMinCode("19337120");
    assertThat(dataOptional).isPresent();
  }

  @Test
  public void testGetPenCoordinatorEmail_givenDifferentInputs_shouldProduceOutput() {
    this.service.init();
    val data = this.service.getPenCoordinatorEmailByMinCode("123546789");
    assertThat(data).isEmpty();
    val dataOptional = this.service.getPenCoordinatorEmailByMinCode("19337120");
    assertThat(dataOptional).isPresent();
    assertThat(dataOptional.get()).isEqualTo("jhamberston0@va.gov");
  }

  @Test
  public void testGetPenCoordinatorEmail_givenDifferentInputsOfMincodeObject_shouldProduceOutput() {
    this.service.init();
    val data = this.service.getPenCoordinatorByMinCode(Mincode.builder().districtNumber(123).schoolNumber(45678).build());
    assertThat(data).isEmpty();
    val dataOptional = this.service.getPenCoordinatorByMinCode(Mincode.builder().districtNumber(193).schoolNumber(37120).build());
    assertThat(dataOptional).isPresent();
  }
}
