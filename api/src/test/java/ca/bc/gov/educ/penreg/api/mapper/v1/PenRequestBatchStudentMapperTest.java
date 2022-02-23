package ca.bc.gov.educ.penreg.api.mapper.v1;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PenRequestBatchStudentMapperTest extends BasePenRegAPITest {

  private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;


  @Test
  public void testToStudentWithDiacriticalMarks() throws IOException {

    // created a student with NA
    final var diacriticalMarks = PenRequestBatchStudent.builder()
      .studentID("566ee980-8e5f-11eb-8dcd-0242ac130003")
      .genderCode("F")
      .legalFirstName("Téd")
      .localID("NA")
      .penRequestBatchStudentID(UUID.randomUUID().toString())
      .legalLastName("Lîúý")
      .legalMiddleNames("Jím")
      .usualFirstName("Stêàlöü")
      .usualLastName("MrÎÙŇ")
      .mincode("345678")
      .build();

    // pass the student to mapper
    final var diacriticalMarksData = studentMapper.toModel(diacriticalMarks);

    assertThat(diacriticalMarksData.getLegalFirstName()).isEqualTo("TED");
    assertThat(diacriticalMarksData.getLegalLastName()).isEqualTo("LIUY");
    assertThat(diacriticalMarksData.getUsualFirstName()).isEqualTo("STEALOU");
    assertThat(diacriticalMarksData.getLegalMiddleNames()).isEqualTo("JIM");
    assertThat(diacriticalMarksData.getUsualLastName()).isEqualTo("MRIUN");
  }

  @Test
  public void testToStudentWithStandardDiacriticalMarks() throws IOException {

    // created a student with NA
    final var diacriticalMarks = PenRequestBatchStudent.builder()
      .studentID("566ee980-8e5f-11eb-8dcd-0242ac130003")
      .genderCode("F")
      .legalFirstName("ÀÁÂÄÅÃÆÇÉÈÊËÍÌÎÏÑÓÒÔÖÕOEÚÙÛÜÝY")
      .localID("NA")
      .penRequestBatchStudentID(UUID.randomUUID().toString())
      .legalLastName("Lîúý")
      .legalMiddleNames("Jím")
      .usualFirstName("Stêàlöü")
      .usualLastName("MrÎÙŇ")
      .mincode("345678")
      .build();

    // pass the student to mapper
    final var diacriticalMarksData = studentMapper.toModel(diacriticalMarks);

    assertThat(diacriticalMarksData.getLegalFirstName()).isEqualTo("AAAAAAÆCEEEEIIIINOOOOOOEUUUUYY");

  }

}
