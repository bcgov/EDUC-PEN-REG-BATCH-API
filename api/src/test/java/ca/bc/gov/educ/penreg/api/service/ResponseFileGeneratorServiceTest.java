package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchRepostReportsFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ResponseFileGeneratorServiceTest extends BasePenRegAPITest {

  @Autowired
  private ResponseFileGeneratorService responseFileGeneratorService;

  @Autowired
  private PenRequestBatchService prbService;
  @Autowired
  private PenRequestBatchRepository prbRepository;

  PenRequestBatchStudentMapper mapper = PenRequestBatchStudentMapper.mapper;

  @Autowired
  RestUtils restUtils;

  protected static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
  protected static final PenRequestBatchMapper batchMapper = PenRequestBatchMapper.mapper;
  protected static final PenRequestBatchStudentMapper batchStudentMapper = PenRequestBatchStudentMapper.mapper;

  private List<PenRequestBatchEntity> batchList;

  @Before
  public void before() {
    Mockito.reset(this.restUtils);
  }

  @Test
  public void testGetPDFBlob_givenBatchFileHasCorrectData_shouldCreateReportBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);
    final var penWebBlob = this.responseFileGeneratorService.getPDFBlob(Base64.getEncoder().encodeToString("here is a pretend pdf".getBytes()), this.batchList.get(0));
    assertThat(penWebBlob).isNotNull();
  }

  @Test
  public void testgetIDSBlob_givenBatchFileHasCorrectStudents_shouldCreateIDSBlob() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    final var penWebBlob = this.responseFileGeneratorService.getIDSBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students);
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518204630109987123456789 JOSEPH                   \r\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E0310210518204630290   123456789 JOSEPH                   \r\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E03102105182046293     123456789 JEBSTER                  \r\n");
    assertThat(new String(penWebBlob.getFileContents(), StandardCharsets.UTF_8)).contains("E031021051822102       123456789 JEY                      \r\n");
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
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFileWithApplicationCodePEN() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
            "mock_pen_req_batch_student_txt.json", 1);
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool(batchList.get(0).getMincode())));

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getSourceApplication()).isEqualTo("MYED");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
    assertThat(new String(penWebBlob.getFileContents())).contains("2046291");
    assertThat(new String(penWebBlob.getFileContents())).contains("221024");
    assertThat(new String(penWebBlob.getFileContents())).doesNotContain("204629298765");
    assertThat(new String(penWebBlob.getFileContents())).contains("PEN");
    assertThat(new String(penWebBlob.getFileContents())).contains("BTR000002");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFileWithApplicationCodeSFAS() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
      "mock_pen_req_batch_student_txt.json", 1);
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool("10200030")));

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
    assertThat(new String(penWebBlob.getFileContents())).contains("2046291");
    assertThat(new String(penWebBlob.getFileContents())).doesNotContain("204629298765");
    assertThat(new String(penWebBlob.getFileContents())).contains("SFAS");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFileWithApplicationCodeMISC() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
      "mock_pen_req_batch_student_txt.json", 1);
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool("10400030")));

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
    assertThat(new String(penWebBlob.getFileContents())).contains("2046291");
    assertThat(new String(penWebBlob.getFileContents())).doesNotContain("204629298765");
    assertThat(new String(penWebBlob.getFileContents())).contains("MISC");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasErrorStudents_shouldCreateTxtFileWithApplicationCodeSS() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
      "mock_pen_req_batch_student_txt.json", 1);
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool(batchList.get(0).getMincode(), "12")));

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()));
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents().length > 0).isTrue();
    assertThat(new String(penWebBlob.getFileContents())).contains("2046291");
    assertThat(new String(penWebBlob.getFileContents())).doesNotContain("204629298765");
    assertThat(new String(penWebBlob.getFileContents())).contains("SS");
  }

  @Test
  @Transactional
  public void testGetTxtFile_givenBatchFileHasNoErrorStudents_shouldCreateEmptyTxtFile() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_txt.json",
      "mock_pen_req_batch_student_txt.json", 1);

    var prbStudents = this.batchList.get(0).getPenRequestBatchStudentEntities().stream()
      .filter(prbStudent -> !prbStudent.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode()))
      .map(mapper::toStructure).collect(Collectors.toList());

    final var penWebBlob = this.responseFileGeneratorService.getTxtBlob(this.batchList.get(0), prbStudents);
    assertThat(penWebBlob).isNotNull();

    assertThat(penWebBlob.getPenWebBlobId()).isEqualTo(penWebBlob.getPenWebBlobId());
    assertThat(penWebBlob.getFileName()).isEqualTo(penWebBlob.getMincode() + ".TXT");
    assertThat(penWebBlob.getFileContents()).isEqualTo("No errors have been identified in this request".getBytes());
  }

  @Test
  @Transactional
  public void testSaveReports_givenPSIBatch_shouldCreatePSIReports() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool(batchList.get(0).getMincode())));

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    this.responseFileGeneratorService.saveReports(Base64.getEncoder().encodeToString("Here's a fake pdf file".getBytes()), this.batchList.get(0),
      this.batchList.get(0).getPenRequestBatchStudentEntities().stream().map(mapper::toStructure).collect(Collectors.toList()), students,
      getPenRequestBatchReportData(this.batchList.get(0)));

    final var penWebBlobsDB = this.prbService.findPenWebBlobBySubmissionNumber(batchList.get(0).getSubmissionNumber());
    assertThat(penWebBlobsDB.size()).isEqualTo(4);
  }

  @Test
  @Transactional
  public void testSaveReports_givenContinuingEdBatch_shouldCreatePARandTXTReports() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);
    this.batchList.get(0).setMincode("03333000");
    this.batchList.get(0).setSchoolGroupCode("02");
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(this.createMockSchool(batchList.get(0).getMincode())));

    final var students = PenRequestBatchTestUtils.createStudents(batchList.get(0));
    this.responseFileGeneratorService.saveReports(Base64.getEncoder().encodeToString("Here's a fake pdf file".getBytes()), this.batchList.get(0),
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
    this.responseFileGeneratorService.savePDFReport(Base64.getEncoder().encodeToString("Here's a fake pdf file".getBytes()), this.batchList.get(0));

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
    var prbStudent = batch.getPenRequestBatchStudentEntities().iterator().next();
    assertThat(new String(content.getFileContents())).contains(prbStudent.getLocalID());
    assertThat(new String(content.getFileContents())).contains(batch.getPenRequestBatchStudentEntities().iterator().next().getLegalLastName());
    assertThat(new String(content.getFileContents())).contains("error issue message");
    assertThat(new String(content.getFileContents())).contains("info request message");
  }

  private PenRequestBatchReportData getPenRequestBatchReportData(PenRequestBatchEntity batch) {
    var issues = batch.getPenRequestBatchStudentEntities().stream().filter(student -> student.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode()))
      .collect(Collectors.toMap(student -> student.getPenRequestBatchStudentID().toString(), student -> "error issue message"));

    List<SchoolContact> studentRegistrationContacts = new ArrayList<>();
    studentRegistrationContacts.add(SchoolContact.builder().email("pen@email.com").firstName("Joe").lastName("Blow").build());

    var sagaData = PenRequestBatchRepostReportsFilesSagaData.builder()
      .penRequestBatchID(batch.getPenRequestBatchID())
      .schoolName(batch.getSchoolName())
      .penRequestBatch(batchMapper.toStructure(batch))
      .penRequestBatchStudents(batch.getPenRequestBatchStudentEntities().stream().map(batchStudentMapper::toStructure).collect(Collectors.toList()))
      .penRequestBatchStudentValidationIssues(issues)
      .students(PenRequestBatchTestUtils.createStudents(batch))
      .studentRegistrationContacts(Arrays.asList(SchoolContact.builder().email("pen@email.com").firstName("Joe").lastName("Blow").build(),
          SchoolContact.builder().email("pen@email2.com").firstName("Joe2").lastName("Blow2").build()))
      .studentRegistrationContacts(studentRegistrationContacts)
      .mailingAddress("123 st")
      .fromEmail("test@email.com")
      .facsimile("5555555555")
      .telephone("2222222222")
      .build();
    return reportMapper.toReportData(sagaData);
  }

  private School createMockSchool(String mincode, String facilityTypeCode) {
    final School school = new School();
    school.setDisplayNameNoSpecialChars("Marco's school");
    school.setMincode(mincode);
    school.setOpenedDate("1964-09-01T00:00:00");
    school.setFacilityTypeCode(facilityTypeCode);
    school.setSchoolNumber(mincode.substring(3));
    return school;
  }

  private School createMockSchool(String mincode) {
    return this.createMockSchool(mincode, "00");
  }
}
