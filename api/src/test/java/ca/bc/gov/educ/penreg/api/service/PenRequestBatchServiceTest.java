package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
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
  public void testgetIDSBlob_givenBatchFileHasCorrectStudents_shouldCreateIDSBlob() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
        "mock_pen_req_batch_student_ids.json", 1);
    when(this.restUtils.getStudentByPEN("123456789")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[0])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[1])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[2])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[3])));

    final var penWebBlob = this.prbService.getIDSBlob(this.batchList.get(0));

    assertThat(penWebBlob).isNotNull();
  }

  @Test
  public void testGetPDFBlob_givenBatchFileHasCorrectData_shouldCreateReportBlob() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    final var penWebBlob = this.prbService.getPDFBlob("here is a pretend pdf", this.batchList.get(0));

    assertThat(penWebBlob).isNotNull();
  }

  @Test
  @Transactional
  public void testSaveReports_givenBatchFileHasCorrectData_shouldSaveReports() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    when(this.restUtils.getStudentByPEN("123456789")).thenReturn(Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[0])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[1])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[2])), Optional.of(JsonUtil.getJsonObjectFromString(Student.class, mockStudents[3])));

    final var penWebBlob = this.prbService.saveReports("here is a pretend pdf", this.batchList.get(0));

    assertThat(penWebBlob).isNotNull();
    assertThat(penWebBlob.size()).isEqualTo(2);
  }

  @Test
  @Transactional
  public void testCreateIDSFile_givenBatchFileHasBadStudents_shouldReturnNull() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
        "mock_pen_req_batch_student_ids_null.json", 1);
    final var penWebBlob = this.prbService.getIDSBlob(this.batchList.get(0));

    assertThat(penWebBlob).isNull();
  }

  public void testGetStats_givenNoDataInDB_shouldReturnTheCountsAsZero() {
    val result = this.prbService.getStats();
    assertThat(result).isNotNull();
    assertThat(result.getPenRequestBatchStatList()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList()).size().isEqualTo(2);
    assertThat(result.getPenRequestBatchStatList().get(0).getSchoolGroupCode()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList().get(0).getSchoolGroupCode()).isEqualTo("K12");
    assertThat(result.getPenRequestBatchStatList().get(0).getFixableCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(0).getRepeatCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(0).getHeldForReviewCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(0).getPendingCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getSchoolGroupCode()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList().get(1).getSchoolGroupCode()).isEqualTo("PSI");
    assertThat(result.getPenRequestBatchStatList().get(1).getFixableCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getRepeatCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getHeldForReviewCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getPendingCount()).isZero();
    assertThat(result.getLoadFailCount()).isZero();
  }

  @Test
  public void testGetStats_givenDataInDB_shouldReturnTheCountsAsInDB() throws IOException {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("API_PEN_REQUEST_BATCH_PEN_REQUEST_BATCH.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().peek(x -> {
      x.setInsertDate(LocalDateTime.now().toString());
      x.setExtractDate(LocalDateTime.now().toString());
    }).map(PenRequestBatchMapper.mapper::toModel).collect(toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(toList());

    this.prbRepository.saveAll(models);
    val result = this.prbService.getStats();
    assertThat(result).isNotNull();
    assertThat(result.getPenRequestBatchStatList()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList()).size().isEqualTo(2);
    assertThat(result.getPenRequestBatchStatList().get(0).getSchoolGroupCode()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList().get(0).getSchoolGroupCode()).isEqualTo("K12");
    assertThat(result.getPenRequestBatchStatList().get(0).getFixableCount()).isEqualTo(13L);
    assertThat(result.getPenRequestBatchStatList().get(0).getRepeatCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(0).getHeldForReviewCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(0).getPendingCount()).isEqualTo(2L);
    assertThat(result.getPenRequestBatchStatList().get(1).getSchoolGroupCode()).isNotEmpty();
    assertThat(result.getPenRequestBatchStatList().get(1).getSchoolGroupCode()).isEqualTo("PSI");
    assertThat(result.getPenRequestBatchStatList().get(1).getFixableCount()).isEqualTo(19L);
    assertThat(result.getPenRequestBatchStatList().get(1).getRepeatCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getHeldForReviewCount()).isZero();
    assertThat(result.getPenRequestBatchStatList().get(1).getPendingCount()).isEqualTo(7L);
    assertThat(result.getLoadFailCount()).isEqualTo(6L);
  }

  @Test
  @Transactional
  public void testUnArchivedStatus_givenDataInDB_shouldBeUpdatedToReArchivedStatus() throws IOException {
    this.batchList = PenRequestBatchUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
            "mock_pen_req_batch_student_ids.json", 1);

    assertThat(this.batchList).isNotEmpty();
    Optional<PenRequestBatchEntity> prbFileOptional = this.prbRepository.findById(this.batchList.get(0).getPenRequestBatchID());
    assertThat(prbFileOptional.isPresent()).isTrue();
    prbFileOptional.get().setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED.getCode());
    this.prbRepository.saveAndFlush(prbFileOptional.get());

    Optional<PenRequestBatchEntity> prbFileDBOptional = this.prbRepository.findById(this.batchList.get(0).getPenRequestBatchID());
    assertThat(prbFileDBOptional.isPresent()).isTrue();
    assertThat(prbFileDBOptional.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.UNARCHIVED.getCode());

    PenRequestBatchEntity requestPrbFile = new PenRequestBatchEntity();
    BeanUtils.copyProperties(prbFileDBOptional.get(), requestPrbFile);
    requestPrbFile.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ARCHIVED.getCode());
    this.prbService.updatePenRequestBatch(requestPrbFile, prbFileDBOptional.get().getPenRequestBatchID());

    PenRequestBatchEntity returnedPrbFile = this.prbRepository.getOne(prbFileDBOptional.get().getPenRequestBatchID());
    assertThat(returnedPrbFile.getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.REARCHIVED.getCode());
  }

}
