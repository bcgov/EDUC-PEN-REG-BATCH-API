package ca.bc.gov.educ.penreg.api.helpers;

import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class PenRegBatchHelperTest {

  @Test
  public void testExactMatch_givenAllValuesSame_shouldReturnTrue() {
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().build();
    Student student = Student.builder().build();
    assertThat(PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)).isTrue();
  }

  @Test
  public void testExactMatch_givenAllValuesSame2_shouldReturnTrue() {
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().genderCode("M").dob("20000101").legalFirstName("test").legalLastName("test").submittedPen("123456789").assignedPEN("123456789").build();
    Student student = Student.builder().sexCode("M").dob("2000-01-01").legalFirstName("test").legalLastName("test").build();
    assertThat(PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)).isTrue();
  }

  @Test
  public void testExactMatch_givenDiffValues_shouldReturnFalse() {
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().genderCode("M").dob("20000101").legalFirstName("test").legalLastName("test").submittedPen("123456789").assignedPEN("123456789").build();
    Student student = Student.builder().sexCode("M").dob("2001-01-01").legalFirstName("test").legalLastName("test").build();
    assertThat(PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)).isFalse();
  }

  @Test
  public void testExactMatch_givenDiffValues2_shouldReturnFalse() {
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().genderCode("F").dob("20000101").legalFirstName("test").legalLastName("test").submittedPen("123456789").assignedPEN("123456789").build();
    Student student = Student.builder().sexCode("M").dob("2000-01-01").legalFirstName("test").legalLastName("test").build();
    assertThat(PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)).isFalse();
  }
}
