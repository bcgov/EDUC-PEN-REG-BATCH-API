package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenRequestBatchServiceTest {

  @Autowired
  private PenRequestBatchService prbService;
  @Autowired
  private PenRequestBatchRepository prbRepository;
  @Autowired
  private PenRequestBatchStudentRepository prbStudentRepository;
  @MockBean
  RestUtils restUtils;

  private List<PenRequestBatchEntity> batchList;

  private static final String [] mockStudents = {
          "{\"studentID\": \"987654321\",\n" +
          " \"pen\": \"123456789\",\n" +
          " \"legalLastName\": \"JOSEPH\",\n" +
          " \"mincode\": \"10210518\",\n" +
          " \"localID\": \"204630109987\",\n" +
          " \"createUser\": \"test\",\n" +
          " \"updateUser\": \"test\"}",
          "{\"studentID\": \"987654321\",\n" +
          " \"pen\": \"123456789\",\n" +
          " \"legalLastName\": \"JOSEPH\",\n" +
          " \"mincode\": \"10210518\",\n" +
          " \"localID\": \"204630290\",\n" +
          " \"createUser\": \"test\",\n" +
          " \"updateUser\": \"test\"}",
          "{\"studentID\": \"987654321\",\n" +
          " \"pen\": \"123456789\",\n" +
          " \"legalLastName\": \"JOSEPH\",\n" +
          " \"mincode\": \"10210518\",\n" +
          " \"localID\": \"2046293\",\n" +
          " \"createUser\": \"test\",\n" +
          " \"updateUser\": \"test\"}",
          "{\"studentID\": \"987654321\",\n" +
          " \"pen\": \"123456789\",\n" +
          " \"legalLastName\": \"JOSEPH\",\n" +
          " \"mincode\": \"10210518\",\n" +
          " \"localID\": \"22102\",\n" +
          " \"createUser\": \"test\",\n" +
          " \"updateUser\": \"test\"}"
  };

  @After
  public void after() {
    this.prbRepository.deleteAll();
    this.prbStudentRepository.deleteAll();
  }

  @Test
  @Transactional
  public void testCreateIDSFile_givenBatchFileHasCorrectStudents_shouldCreateIDSFile() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
        "mock_pen_req_batch_student_ids.json", 1);
    when(this.restUtils.getStudentByPEN("123456789")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[0])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[1])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[2])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[3])));

    final var penWebBlob = this.prbService.createIDSFile(this.batchList.get(0));

    assertThat(penWebBlob).isNotNull();
//    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
//    assertThat(penRequestBatch.get().getFixableCount()).isZero();
  }

  @Test
  @Transactional
  public void testCreateIDSFile_givenBatchFileHasBadStudents_shouldReturnNull() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
        "mock_pen_req_batch_student_ids_null.json", 1);
    final var penWebBlob = this.prbService.createIDSFile(this.batchList.get(0));

    assertThat(penWebBlob).isNull();
//    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
//    assertThat(penRequestBatch.get().getFixableCount()).isZero();
  }
}
