package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchRepostReportsFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

public class ResponseFileGeneratorServiceTest extends BasePenRegAPITest {

  @Autowired
  private ResponseFileGeneratorService responseFileGeneratorService;

  @Autowired
  private PenRequestBatchService prbService;
  @Autowired
  private PenRequestBatchRepository prbRepository;
  @Autowired
  private PenRequestBatchStudentRepository prbStudentRepository;

  PenRequestBatchStudentMapper mapper = PenRequestBatchStudentMapper.mapper;

  @Autowired
  RestUtils restUtils;
  @Mock
  private PenCoordinatorService penCoordinatorService;

  protected static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
  protected static final PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
  protected static final PenRequestBatchStudentMapper batchStudentMapper = PenRequestBatchStudentMapper.mapper;

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


  @Test
  public void testGetPDFBlob_givenBatchFileHasCorrectData_shouldCreateReportBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);
    final var penWebBlob = this.responseFileGeneratorService.getPDFBlob("here is a pretend pdf", this.batchList.get(0));
    assertThat(penWebBlob).isNotNull();
  }

  @Test
  public void testgetIDSBlob_givenBatchFileHasCorrectStudents_shouldCreateIDSBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    final var penWebBlob = this.responseFileGeneratorService.getIDSBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518204630109987123456789 JOSEPH\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000204630290123456789 JOSEPH\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000002046293123456789 JEBSTER\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518000000022102123456789 JEY\n");
  }

  @Test
  public void testgetIDSBlob_givenBatchFileHasCorrectWrongStudents_shouldCreateEmptyIDSBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids_null.json", 1);

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    final var penWebBlob = this.responseFileGeneratorService.getIDSBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).isEqualTo("No NEW PENS have been assigned by this PEN request");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFile() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
            "mock_pen_req_batch_student_txt.json", 1);
    doReturn(Optional.of(JsonUtil.getJsonObjectFromString(PenCoordinator.class, mockCoordinator))).when(this.penCoordinatorService).getPenCoordinatorByMinCode("10210518");

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
  }

  @Test
  @Transactional
  public void testSaveReports_givenPSIBatch_shouldCreatePSIReports() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    this.responseFileGeneratorService.saveReports("Here's a fake pdf file", this.batchList.get(0),
      this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students,
      getPenRequestBatchReportData(this.batchList.get(0)));

    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(batchList.get(0).getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(4);
  }

  @Test
  @Transactional
  public void testSavePDFReport_givenPDFString_and_Batch_shouldCreatePDFReport() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    this.batchList.get(0).setSubmissionNumber("M001");
    this.responseFileGeneratorService.savePDFReport("Here's a fake pdf file", this.batchList.get(0));

    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(batchList.get(0).getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testCreateParFile_givenPdfFileData_shouldParFilePenWebBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);

    var batch = this.batchList.get(0);

    var reportData = getPenRequestBatchReportData(batch);

    final var content = this.responseFileGeneratorService.getPARBlob(reportData, batch);

    assertThat(content).isNotNull();
    assertThat(content.getFileType()).isEqualTo("PAR");
    assertThat(new String(content.getFileContents())).contains(batch.getMincode());
    assertThat(new String(content.getFileContents())).contains(batch.getPenRequestBatchStudentEntities().iterator().next().getLegalLastName());
  }

  private PenRequestBatchReportData getPenRequestBatchReportData(PenRequestBatchEntity batch) {
    var sagaData = PenRequestBatchRepostReportsFilesSagaData.builder()
      .penRequestBatchID(batch.getPenRequestBatchID())
      .schoolName(batch.getSchoolName())
      .penRequestBatch(batchMapper.toStructure(batch))
      .penRequestBatchStudents(batch.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .students(PenRequestBatchTestUtils.createStudents(batch))
      .penCordinatorEmail("pen@email.com")
      .mailingAddress("123 st")
      .fromEmail("test@email.com")
      .facsimile("5555555555")
      .telephone("2222222222")
      .build();
    return reportMapper.toReportData(sagaData);
  }
}
