package ca.bc.gov.educ.penreg.api.mapper.v1;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PenRequestBatchFileMapperTest extends BasePenRegAPITest {

  private static final PenRequestBatchFileMapper fileMapper = PenRequestBatchFileMapper.mapper;

  @Test
  public void testToBatchStudentEntityWith10DigitPEN_ShouldNotReturnPEN() throws IOException {
    StudentDetails studentDetails = new StudentDetails();
    studentDetails.setPen("1234567890");
    final var entity = fileMapper.toPenRequestBatchStudentEntity(studentDetails, new PenRequestBatchEntity());
    assertThat(entity.getSubmittedPen()).isNull();
  }

  @Test
  public void testToBatchStudentEntityWithPEN_ShouldReturnPEN() throws IOException {
    StudentDetails studentDetails = new StudentDetails();
    studentDetails.setPen("123456789");
    final var entity = fileMapper.toPenRequestBatchStudentEntity(studentDetails, new PenRequestBatchEntity());
    assertThat(entity.getSubmittedPen()).isEqualTo("123456789");
  }

}
