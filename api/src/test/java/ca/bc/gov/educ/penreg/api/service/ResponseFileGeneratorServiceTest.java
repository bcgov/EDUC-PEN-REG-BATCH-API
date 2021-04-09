package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

  PenRequestBatchStudentMapper mapper = PenRequestBatchStudentMapper.mapper;

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
          "{\"studentID\": \"987654322\",\n" +
                  " \"pen\": \"123456789\",\n" +
                  " \"legalLastName\": \"JOSEPH\",\n" +
                  " \"mincode\": \"10210518\",\n" +
                  " \"localID\": \"204630290\",\n" +
                  " \"createUser\": \"test\",\n" +
                  " \"updateUser\": \"test\"}",
          "{\"studentID\": \"987654323\",\n" +
                  " \"pen\": \"123456789\",\n" +
                  " \"legalLastName\": \"JOSEPH\",\n" +
                  " \"mincode\": \"10210518\",\n" +
                  " \"localID\": \"2046293\",\n" +
                  " \"createUser\": \"test\",\n" +
                  " \"updateUser\": \"test\"}",
          "{\"studentID\": \"987654324\",\n" +
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
  public void testGetPDFBlob_givenBatchFileHasCorrectData_shouldCreateReportBlob() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);
    final var penWebBlob = this.responseFileGeneratorService.getPDFBlob("here is a pretend pdf", this.batchList.get(0));
    assertThat(penWebBlob).isNotNull();
  }

  @Test
  public void testgetIDSBlob_givenBatchFileHasCorrectStudents_shouldCreateIDSBlob() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchUtils.createStudents(batchList.get(0));
    final var penWebBlob = this.responseFileGeneratorService.getIDSBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518204630109987123456789 JOSEPH\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000204630290123456789 JOSEPH\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000002046293123456789 JEBSTER\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000000022102123456789 JEY\n");
  }

  @Test
  public void testgetIDSBlob_givenBatchFileHasCorrectWrongStudents_shouldCreateEmptyIDSBlob() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids_null.json", 1);

    final var students = PenRequestBatchUtils.createStudents(batchList.get(0));
    final var penWebBlob = this.responseFileGeneratorService.getIDSBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).isEqualTo("No NEW PENS have been assigned by this PEN request");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFile() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
            "mock_pen_req_batch_student_txt.json", 1);
    when(this.penCoordinatorService.getPenCoordinatorByMinCode("10210518")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(PenCoordinator.class, mockCoordinator)));

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
  }

  @Test
  @Transactional
  public void testSaveReports_givenPSIBatch_shouldCreatePSIReports() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchUtils.createStudents(batchList.get(0));
    this.responseFileGeneratorService.saveReports("Here's a fake pdf file", this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);

    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(batchList.get(0).getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(3);
  }
}
