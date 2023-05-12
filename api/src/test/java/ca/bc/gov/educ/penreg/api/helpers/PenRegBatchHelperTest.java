package ca.bc.gov.educ.penreg.api.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

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
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().genderCode("M").dob("20000101").legalMiddleNames("middleName 123").legalFirstName("test").legalLastName("test").submittedPen("123456789").assignedPEN("123456789").build();
    Student student = Student.builder().sexCode("M").dob("2000-01-01").legalFirstName("test").legalMiddleNames("middleName 123").legalLastName("test").build();
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

  @Test
  public void testExactMatch_givenDiffValues3_shouldReturnFalse() {
    PenRequestBatchStudent penRequestBatchStudent = PenRequestBatchStudent.builder().genderCode("F").dob("20000101").legalFirstName("test").legalMiddleNames("AARON").legalLastName("test").submittedPen("123456789").assignedPEN("123456789").build();
    Student student = Student.builder().sexCode("M").dob("2000-01-01").legalFirstName("test").legalLastName("test").build();
    assertThat(PenRegBatchHelper.exactMatch(penRequestBatchStudent, student)).isFalse();
  }
}
