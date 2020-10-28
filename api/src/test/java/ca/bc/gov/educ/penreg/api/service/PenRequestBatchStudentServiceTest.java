package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenRequestBatchStudentServiceTest {
  @Autowired
  private PenRequestBatchStudentService prbStudentService;
  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  private UUID penRequestBatchID;

  private UUID penRequestBatchStudentID;

  private List<PenRequestBatchEntity> batchList;

  @Before
  public void setUp() throws IOException {
    batchList = PenRequestBatchUtils.createBatchStudents(penRequestBatchRepository, "mock_pen_req_batch_archived.json",
      "mock_pen_req_batch_student_archived.json", 1);
    penRequestBatchID = batchList.get(0).getPenRequestBatchID();
  }

  @After
  public void after() {
    penRequestBatchRepository.deleteAll();
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromFixableToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    penRequestBatchStudentID = getFirstPenRequestBatchStudentID(FIXABLE.getCode());

    var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    var updatedPrbStudent = prbStudentService.updateStudent(prbStudentEntity, penRequestBatchID, penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    var penRequestBatch = penRequestBatchRepository.findById(penRequestBatchID);
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getFixableCount()).isEqualTo(0);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromMatchedSysToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    penRequestBatchStudentID = getFirstPenRequestBatchStudentID(SYS_MATCHED.getCode());

    var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    var updatedPrbStudent = prbStudentService.updateStudent(prbStudentEntity, penRequestBatchID, penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    var penRequestBatch = penRequestBatchRepository.findById(penRequestBatchID);
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getMatchedCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromMatchedUsrToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    penRequestBatchStudentID = getFirstPenRequestBatchStudentID(USR_MATCHED.getCode());

    var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    var updatedPrbStudent = prbStudentService.updateStudent(prbStudentEntity, penRequestBatchID, penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    var penRequestBatch = penRequestBatchRepository.findById(penRequestBatchID);
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getMatchedCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromInforeqToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    penRequestBatchStudentID = getFirstPenRequestBatchStudentID(INFOREQ.getCode());

    var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    var updatedPrbStudent = prbStudentService.updateStudent(prbStudentEntity, penRequestBatchID, penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    var penRequestBatch = penRequestBatchRepository.findById(penRequestBatchID);
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getErrorCount()).isEqualTo(1);
  }

  @Test
  @Transactional
  public void testUpdatePenRequestBatchStudent_givenStatusChangeFromRepeatToNewPenUsr_shouldUpdatePrbStudentAndPrb() throws IOException {
    penRequestBatchStudentID = getFirstPenRequestBatchStudentID(REPEAT.getCode());

    var prbStudentEntity = JsonUtil.getJsonObjectFromString(PenRequestBatchStudentEntity.class, dummyPenRequestBatchStudentDataJson(USR_NEW_PEN.toString()));
    prbStudentEntity.setUpdateDate(LocalDateTime.now());

    var updatedPrbStudent = prbStudentService.updateStudent(prbStudentEntity, penRequestBatchID, penRequestBatchStudentID);

    assertThat(updatedPrbStudent.getPenRequestBatchStudentStatusCode()).isEqualTo(USR_NEW_PEN.toString());

    var penRequestBatch = penRequestBatchRepository.findById(penRequestBatchID);
    assertThat(penRequestBatch.get().getNewPenCount()).isEqualTo(3);
    assertThat(penRequestBatch.get().getRepeatCount()).isEqualTo(0);
  }

  protected String dummyPenRequestBatchStudentDataJson(String status) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"penRequestBatchStudentStatusCode\": \"" + status + "\",\n" +
      "    \"genderCode\": \"X\"\n" +
      "  }";
  }

  private UUID getFirstPenRequestBatchStudentID(String status) {
    return batchList.get(0).getPenRequestBatchStudentEntities().stream()
      .filter(student -> student.getPenRequestBatchStudentStatusCode().equals(status)).findFirst().get().getPenRequestBatchStudentID();
  }
}
