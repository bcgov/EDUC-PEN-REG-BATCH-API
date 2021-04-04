package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * The type Pen request batch student orchestrator service test.
 */
@RunWith(JUnitParamsRunner.class)
@Slf4j
public class PenRequestBatchStudentOrchestratorServiceTest extends BasePenRegAPITest {
  /**
   * The constant scr.
   */
  @ClassRule
  public static final SpringClassRule scr = new SpringClassRule();

  /**
   * The Smr.
   */
  @Rule
  public final SpringMethodRule smr = new SpringMethodRule();

  /**
   * The Orchestrator service.
   */
  @Autowired
  private PenRequestBatchStudentOrchestratorService orchestratorService;

  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Before
  public void setup() throws IOException {
    final File mockPenReqBatch = new File("src/test/resources/mock_pen_req_batch.json");
    final List<PenRequestBatch> penRequestBatches = new ObjectMapper().readValue(mockPenReqBatch, new TypeReference<>() {
    });
    final File mockPenReqBatchStudent = new File("src/test/resources/mock_pen_req_batch_student.json");
    final List<PenRequestBatchStudent> penRequestBatchStudents = new ObjectMapper().readValue(mockPenReqBatchStudent, new TypeReference<>() {
    });
    final PenRequestBatchEntity entity = PenRequestBatchMapper.mapper.toModel(penRequestBatches.get(0));
    for (final var prbStudent : penRequestBatchStudents) {
      final var studentEntity = PenRequestBatchStudentMapper.mapper.toModel(prbStudent);
      studentEntity.setCreateDate(LocalDateTime.now());
      studentEntity.setCreateUser("TEST");
      studentEntity.setUpdateUser("TEST");
      studentEntity.setUpdateDate(LocalDateTime.now());
      studentEntity.setPenRequestBatchEntity(entity);
      entity.getPenRequestBatchStudentEntities().add(studentEntity);
    }
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser("TEST");
    entity.setUpdateUser("TEST");
    this.penRequestBatchRepository.save(entity);
  }

  /**
   * Test are both field value equal.
   *
   * @param field1 the field 1
   * @param field2 the field 2
   * @param res    the res
   */
  @Test
  @Parameters({
      "null, null, true",
      ",, true",
      "hi,hi, true",
      "hello,hello, true",
      "hello,Hello, false",
      "hello,hi, false",
  })
  public void testAreBothFieldValueEqual(String field1, String field2, final boolean res) {
    if ("null".equals(field1)) {
      field1 = null;
    }
    if ("null".equals(field2)) {
      field2 = null;
    }
    final var result = this.orchestratorService.areBothFieldValueEqual(field1, field2);
    assertThat(result).isEqualTo(res);

  }

  /**
   * Scrub name field.
   *
   * @param fieldValue    the field value
   * @param scrubbedValue the scrubbed value
   */
  @Test
  @Parameters({
      "a,A",
      "a  ,A",
      "hi\t,HI",
      "hello.,HELLO",
      "  he    llo      ,HE LLO",
  })
  public void scrubNameField(final String fieldValue, final String scrubbedValue) {
    final var result = this.orchestratorService.scrubNameField(fieldValue);
    assertThat(result).isEqualTo(scrubbedValue);
  }
}
