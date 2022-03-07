package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static java.util.stream.Collectors.joining;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Message publisher test.
 */
public class PenRequestBatchStudentServiceTest extends BasePenRegAPITest {
  @Autowired
  private PenRequestBatchStudentService prbStudentService;
  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  private UUID penRequestBatchID;

  private UUID penRequestBatchStudentID;

  private List<PenRequestBatchEntity> batchList;

  @Before
  public void setUp() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_archived.json",
        "mock_pen_req_batch_student_archived.json", 1);
    this.penRequestBatchID = this.batchList.get(0).getPenRequestBatchID();
  }


  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromFixableToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(FIXABLE.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    final var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getFixableCount()).isZero();
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromMatchedSysToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(SYS_MATCHED.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    final var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getMatchedCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromMatchedUsrToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(USR_MATCHED.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    final var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getMatchedCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromInforeqToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(INFOREQ.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    final var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getErrorCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromRepeatToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(REPEAT.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    final var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getRepeatCount()).isZero();
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromRepeatToUserMatched_shouldUpdatePrbStudentAndPrbWithUnarchivedChangedStatus() throws IOException {
    var penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    final var penRequestBatchEntity = penRequestBatch.get();
    penRequestBatchEntity.setPenRequestBatchStatusCode(PenRequestBatchStatusCodes.UNARCHIVED.getCode());
    this.penRequestBatchRepository.save(penRequestBatchEntity);

    this.penRequestBatchStudentID = this.getFirstPenRequestBatchStudentID(REPEAT.getCode());

    final var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, this.dummyPenRequestBatchStudentDataJson(USR_MATCHED.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    final var updatedPrbStudent = this.prbStudentService.updateStudent(prbStudentEntity, this.penRequestBatchID, this.penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_MATCHED.toString());

    penRequestBatch = this.penRequestBatchRepository.findById(this.penRequestBatchID);
    assertThat(penRequestBatch).isPresent();
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(2);
    assertThat(penRequestBatch.get().getRepeatCount()).isZero();
    assertThat(penRequestBatch.get().getPenRequestBatchStatusCode()).isEqualTo(PenRequestBatchStatusCodes.UNARCHIVED_CHANGED.getCode());
  }

  protected String dummyPenRequestBatchStudentDataJson(final String status) {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + this.penRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\",\n" +
        "    \"penRequestBatchStudentStatusCode\": \"" + status + "\",\n" +
        "    \"genderCode\": \"X\"\n" +
        "  }";
  }

  private UUID getFirstPenRequestBatchStudentID(final String status) {
    return this.batchList.get(0).getPenRequestBatchStudentEntities().stream()
        .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(status)).findFirst().orElseThrow().getPenRequestBatchStudentID();
  }

  @Test
  @Transactional
  public void testGetAllSamePensWithinPenRequestBatchByID_givenSameAssignedPens_shouldReturnResults() throws IOException {
    this.batchList = PenRequestBatchTestUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch_for_same_student.json",
        "mock_pen_req_batch_student_with_same_student_id.json", 2);

    assertThat(this.batchList).isNotEmpty();
    var batchIds = this.batchList.stream()
        .map((prbEntity) -> prbEntity.getPenRequestBatchID().toString())
        .collect(joining(","));

    var studentIds = this.prbStudentService.getAllSamePensWithinPenRequestBatchByID(batchIds);

    assertThat(studentIds.size()).isEqualTo(8);
  }

}
