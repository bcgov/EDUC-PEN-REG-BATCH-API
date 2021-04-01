package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class ResponseFileGeneratorServiceTest {

  @Autowired
  private ResponseFileGeneratorService responseFileGeneratorService;

  @Autowired
  private PenRequestBatchService prbService;
  @Autowired
  private PenRequestBatchRepository prbRepository;
  @Autowired
  private PenRequestBatchStudentRepository prbStudentRepository;

  @MockBean
  RestUtils restUtils;
  @MockBean
  private PenCoordinatorService penCoordinatorService;

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

  private static final String mockMincode = "{\n" +
          "    \"districtNumber\": 102,\n" +
          "    \"schoolNumber\": 10518\n" +
          "  }";

  private static final String mockCoordinator = "{\n" +
          "    \"mincode\":" +  mockMincode + ",\n" +
          "    \"penCoordinatorName\": \"Jenni Hamberston\",\n" +
          "    \"penCoordinatorEmail\": \"jhamberston0@va.gov\",\n" +
          "    \"penCoordinatorFax\": \"780-308-6528\",\n" +
          "    \"sendPenResultsVia\": \"E\"\n" +
          "  }";

  @After
  public void after() {
    this.prbStudentRepository.deleteAll();
    this.prbRepository.deleteAll();
  }

  @Test
  @Transactional
  public void testCreateIDSFile_givenBatchFileHasCorrectStudents_shouldCreateIDSFile() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);
    when(this.restUtils.getStudentByPEN("123456789")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[0])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[1])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[2])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[3])));

    final var penWebBlob = this.responseFileGeneratorService.createIDSFile(this.batchList.get(0));

    assertThat(penWebBlob).isNotNull();
  }

  @Test
  @Transactional
  public void testCreateIDSFile_givenBatchFileHasBadStudents_shouldReturnNull() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids_null.json", 1);
    final var penWebBlob = this.responseFileGeneratorService.createIDSFile(this.batchList.get(0));

    assertThat(penWebBlob).isNull();
//    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
//    assertThat(penRequestBatch.get().getFixableCount()).isZero();
  }

  @Test
  @Transactional
  public void testCreateTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFile() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
            "mock_pen_req_batch_student_txt.json", 1);
    when(this.penCoordinatorService.getPenCoordinatorByMinCode("10210518")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(PenCoordinator.class, mockCoordinator)));

    final var penWebBlob = this.responseFileGeneratorService.createTxtFile(this.batchList.get(0));
    assertThat(penWebBlob).isNotNull();

    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumberAndFileType(penWebBlob.getSubmissionNumber(), "TXT");
    assertThat(penWebBlobsDB.isEmpty()).isFalse();
    assertThat(penWebBlobsDB.get(0).getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlobsDB.get(0).getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlobsDB.get(0).getFileContents().length > 0).isTrue();
  }
}
