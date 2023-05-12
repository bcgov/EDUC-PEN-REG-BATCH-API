package ca.bc.gov.educ.penreg.api.service;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestIDs;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class PenRequestBatchServiceTest extends BasePenRegAPITest {

  @Autowired
  private PenRequestBatchService prbService;
  @Autowired
  private PenRequestBatchRepository prbRepository;
  @Autowired
  private PenRequestBatchStudentRepository prbStudentRepository;
  @Autowired
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
    }).map(PenRequestBatchMapper.mapper::toModel).collect(toList()).stream().map(PenRequestBatchTestUtils::populateAuditColumns).collect(toList());

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
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);

    assertThat(this.batchList).isNotEmpty();
    final Optional<PenRequestBatchEntity> prbFileOptional = this.prbRepository.findById(this.batchList.get(0).getPenRequestBatchID());
    assertThat(prbFileOptional.isPresent()).isTrue();
    prbFileOptional.get().setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED.getCode());
    this.prbRepository.saveAndFlush(prbFileOptional.get());

    final Optional<PenRequestBatchEntity> prbFileDBOptional = this.prbRepository.findById(this.batchList.get(0).getPenRequestBatchID());
    assertThat(prbFileDBOptional.isPresent()).isTrue();
    assertThat(prbFileDBOptional.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.UNARCHIVED.getCode());

    final PenRequestBatchEntity requestPrbFile = new PenRequestBatchEntity();
    BeanUtils.copyProperties(prbFileDBOptional.get(), requestPrbFile);
    requestPrbFile.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.ARCHIVED.getCode());
    this.prbService.updatePenRequestBatch(requestPrbFile, prbFileDBOptional.get().getPenRequestBatchID());

    final PenRequestBatchEntity returnedPrbFile = this.prbRepository.getOne(prbFileDBOptional.get().getPenRequestBatchID());
    assertThat(returnedPrbFile.getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.REARCHIVED.getCode());
  }

  @Test
  @Transactional
  public void testPopulateStudentDataFromBatch_shouldReturnResults() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 3);

    when(this.restUtils.getStudentsByStudentIDs(any())).thenReturn(PenRequestBatchTestUtils.createStudentsForNotNullPRBStudents(this.batchList.get(0)));

    assertThat(this.batchList).isNotEmpty();
    var batchIds = this.batchList.stream()
      .map(PenRequestBatchEntity::getPenRequestBatchID)
      .collect(toList());
    val results = this.prbService.populateStudentDataFromBatch(batchList.get(0));
    assertThat(results.size()).isEqualTo(4);
  }

  @Test
  @Transactional
  public void testFindAllPenRequestIDs_givenNullSearchCriteria_shouldReturnPenRequestIDsList() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);

    assertThat(this.batchList).isNotEmpty();
    var batchIds = this.batchList.stream()
      .map(PenRequestBatchEntity::getPenRequestBatchID)
      .collect(toList());
    List<PenRequestIDs> ids = this.prbService.findAllPenRequestIDs(batchIds, List.of(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(), PenRequestBatchStudentStatusCodes.INFOREQ.getCode()), null);
    assertThat(ids.size()).isEqualTo(3);
    assertThat(ids.get(0).getPenRequestBatchID()).isEqualTo(batchIds.get(0));
  }

  @Test
  @Transactional
  public void testFindAllPenRequestIDs_givenAllSearchCriteria_shouldReturnPenRequestIDsList() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.prbRepository, "mock_pen_req_batch_ids.json",
      "mock_pen_req_batch_student_ids.json", 1);

    assertThat(this.batchList).isNotEmpty();
    var batchIds = this.batchList.stream()
      .map(PenRequestBatchEntity::getPenRequestBatchID)
      .collect(toList());

    Map<String,String> searchCriteria = new HashMap<>();
    searchCriteria.put("mincode", "10210518");
    searchCriteria.put("localID", "1488645");
    searchCriteria.put("submittedPen", "123456789");
    searchCriteria.put("legalSurname", "JOHNSTON");
    searchCriteria.put("legalGivenName", "ANGEL");
    searchCriteria.put("legalMiddleNames", "MARIA LYNN");
    searchCriteria.put("usualSurname", "JEB");
    searchCriteria.put("usualGivenName", "JEB");
    searchCriteria.put("usualMiddleNames", "JEB");
    searchCriteria.put("dob", "20060628");
    searchCriteria.put("gender", "F");
    searchCriteria.put("grade", "02");
    searchCriteria.put("postalCode", "Y1A4V1");
    searchCriteria.put("bestMatchPEN", "123456789");
    searchCriteria.put("submissionNumber", "T-534093");

    List<PenRequestIDs> ids = this.prbService.findAllPenRequestIDs(batchIds,
      List.of(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode(),
      PenRequestBatchStudentStatusCodes.FIXABLE.getCode()),
      searchCriteria);
    assertThat(ids.size()).isEqualTo(1);
    assertThat(ids.get(0).getPenRequestBatchID()).isEqualTo(batchIds.get(0));
  }

}
