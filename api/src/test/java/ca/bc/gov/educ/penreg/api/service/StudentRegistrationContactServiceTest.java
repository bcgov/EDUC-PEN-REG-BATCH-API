package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class StudentRegistrationContactServiceTest extends BasePenRegAPITest {
  private final Map<String, List<SchoolContact>> studentRegistrationContactMap = new ConcurrentHashMap<>();

  @Autowired
  StudentRegistrationContactService service;

  @Autowired
  RestUtils restUtils;

  @Before
  public void setup() throws IOException {
    Mockito.reset(restUtils);
    final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-student-registration-contact.json")).getFile());
    final List<SchoolContact> structs = new ObjectMapper().readValue(file, new TypeReference<>() {});
    studentRegistrationContactMap.putAll(structs.stream().collect(Collectors.groupingBy(SchoolContact::getSchoolId)));
  }

  @Test
  public void testGetStudentRegistrationContactList_givenDifferentInputs_shouldProduceOutput() {
    String mincodeNotExist = "123546789";
    when(this.restUtils.getStudentRegistrationContactList(mincodeNotExist)).thenReturn(studentRegistrationContactMap.get(mincodeNotExist));
    var data = this.service.getStudentRegistrationContactsByMincode(mincodeNotExist);
    assertThat(data).isEmpty();

    String mindcodeExists = "12345678";
    when(this.restUtils.getStudentRegistrationContactList(mindcodeExists)).thenReturn(studentRegistrationContactMap.get(mindcodeExists));
    var dataList = this.service.getStudentRegistrationContactEmailsByMincode(mindcodeExists);
    assertThat(dataList).hasSize(2);
  }

  @Test
  public void testGetStudentRegistrationContactEmailsByMincode_givenDifferentInputs_shouldProduceOutput() {
    String mincodeExists = "11111111";
    when(this.restUtils.getStudentRegistrationContactList(mincodeExists)).thenReturn(studentRegistrationContactMap.get(mincodeExists));
    var dataList = this.service.getStudentRegistrationContactEmailsByMincode(mincodeExists);
    assertThat(dataList).hasSize(1);
    assertThat(dataList.get(0)).isEqualTo("fake@gmail.com");

    String mincodeNotExist = "123546789";
    when(this.restUtils.getStudentRegistrationContactList(mincodeNotExist)).thenReturn(studentRegistrationContactMap.get(mincodeNotExist));
    var dataList2 = this.service.getStudentRegistrationContactEmailsByMincode(mincodeNotExist);
    assertThat(dataList2).isEmpty();
  }
}
