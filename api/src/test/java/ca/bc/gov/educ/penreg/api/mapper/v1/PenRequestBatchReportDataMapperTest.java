package ca.bc.gov.educ.penreg.api.mapper.v1;

import static org.assertj.core.api.Assertions.assertThat;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PenRequestBatchReportDataMapperTest extends BasePenRegAPITest {

  private static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;
  @Autowired
  private PenRequestBatchRepository repository;

  @Test
  public void testToReportUserMatchedListItem_GivenAllValues_ShouldMapSuccessfully() throws IOException {
    final var batchEntities = PenRequestBatchTestUtils.createBatchStudents(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_archived_with_pen.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.parse("2021-03-23T13:04:48.840098")));

    final var student1 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130002").dob("1990-07-04").genderCode("M").legalFirstName("Mike").pen("123456785").legalLastName("Joe").legalMiddleNames("Tim").usualFirstName("Bob").usualLastName("Smithy").usualMiddleNames("Smalls").mincode(batchEntities.get(0).getMincode()).demogCode("C").build();
    final var student3 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130009").dob("2011-12-07").genderCode("M").sexCode("M").legalFirstName("BRAYDON").pen("123456784").legalLastName("JOSEPH").legalMiddleNames("JAMIESON").usualFirstName("BRAYDON").usualLastName("JOSEPH").usualMiddleNames(null).mincode(batchEntities.get(0).getMincode()).demogCode("A").build();
    final List<Student> students = new ArrayList<>();
    students.add(student1);
    students.add(student3);

    final var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder()
      .facsimile("3333333333")
      .telephone("5555555555")
      .fromEmail("test@abc.com")
      .mailingAddress("mailing address")
      .studentRegistrationContacts(PenCoordinator.builder().penCoordinatorEmail("test@email.com").penCoordinatorName("Joe Blow").build())
      .schoolName("Cataline")
      .penRequestBatch(mapper.toStructure(batchEntities.get(0)))
      .penRequestBatchStudents(batchEntities.get(0).getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList()))
      .penRequestBatchStudentValidationIssues(new HashMap<>())
      .students(students).build();

    final PenRequestBatchReportData reportData = reportMapper.toReportData(sagaData);

    assertThat(reportData.getProcessDate()).isEqualTo("2021/03/23");
    assertThat(reportData.getProcessTime()).isEqualTo("13:04");
    assertThat(reportData.getSubmissionNumber()).isEqualTo(batchEntities.get(0).getSubmissionNumber());
    assertThat(reportData.getReportDate()).isEqualTo("2021-MAR-23");
    assertThat(reportData.getReviewer()).isEqualTo("Joe Blow");
    assertThat(reportData.getMincode()).isEqualTo(formatMincode(batchEntities.get(0).getMincode()));
    assertThat(reportData.getSchoolName()).isEqualTo("Cataline");
    assertThat(reportData.getFascimile()).isEqualTo("3333333333");
    assertThat(reportData.getTelephone()).isEqualTo("5555555555");
    assertThat(reportData.getMailingAddress()).isEqualTo("mailing address");
    assertThat(reportData.getPenCordinatorEmail()).isEqualTo("test@abc.com");

    assertThat(reportData.getDiffList().size()).isEqualTo(1);
    assertThat(reportData.getDiffList().get(0).getMin().getBirthDate()).isEqualTo("1990/07/04");
    assertThat(reportData.getDiffList().get(0).getMin().getGender()).isEqualTo("M");
    assertThat(reportData.getDiffList().get(0).getMin().getGivenName()).isEqualTo("Mike");
    assertThat(reportData.getDiffList().get(0).getMin().getLegalMiddleNames()).isEqualTo("Tim");
    assertThat(reportData.getDiffList().get(0).getMin().getPen()).isEqualTo("123456785");
    assertThat(reportData.getDiffList().get(0).getMin().getReason()).isNull();
    assertThat(reportData.getDiffList().get(0).getMin().getSchoolID()).isBlank();
    assertThat(reportData.getDiffList().get(0).getMin().getSurname()).isEqualTo("Joe");
    assertThat(reportData.getDiffList().get(0).getMin().getUsualName()).isEqualTo("JOSEPH, BRAYDON, KIM");

    assertThat(reportData.getDiffList().get(0).getSchool().getBirthDate()).isEqualTo("2011/12/08");
    assertThat(reportData.getDiffList().get(0).getSchool().getGender()).isEqualTo("M");
    assertThat(reportData.getDiffList().get(0).getSchool().getGivenName()).isEqualTo("BRAYDON");
    assertThat(reportData.getDiffList().get(0).getSchool().getLegalMiddleNames()).isEqualTo("JAMIESON");
    assertThat(reportData.getDiffList().get(0).getSchool().getPen()).isEqualTo("123456785");
    assertThat(reportData.getDiffList().get(0).getSchool().getReason()).isEqualTo("Here's some info");
    assertThat(reportData.getDiffList().get(0).getSchool().getSchoolID()).isEqualTo("2046302");
    assertThat(reportData.getDiffList().get(0).getSchool().getSurname()).isEqualTo("JOSEPH");
    assertThat(reportData.getDiffList().get(0).getSchool().getUsualName()).isEqualTo("JOSEPH, BRAYDON, KIM");

    assertThat(reportData.getNewPenList().size()).isEqualTo(1);
    assertThat(reportData.getPendingList().size()).isEqualTo(5);
    assertThat(reportData.getSysMatchedList().size()).isEqualTo(1); // exact match
}

  @Test
  public void testToReportUserMatchedListItem_GivenBadDOB_ShouldMapSuccessfully() throws IOException {
    final var batchEntities = PenRequestBatchTestUtils.createBatchStudents(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_bad_dob.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.parse("2021-03-23T13:04:48.840098")));

    final var student = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130003").dob("19900734").genderCode("F").legalFirstName("Ted").pen("123456780").legalLastName("Jones").legalMiddleNames("Jim").usualFirstName("Steal").usualLastName("Mr").usualMiddleNames("Yo Girl").mincode(batchEntities.get(0).getMincode()).demogCode("A").build();
    final List<Student> students = new ArrayList<>();
    students.add(student);

    final var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder()
      .facsimile("3333333333")
      .telephone("5555555555")
      .fromEmail("test@abc.com")
      .mailingAddress("mailing address")
      .studentRegistrationContacts(PenCoordinator.builder().penCoordinatorEmail("test@email.com").penCoordinatorName("Joe Blow").build())
      .schoolName("Cataline")
      .penRequestBatch(mapper.toStructure(batchEntities.get(0)))
      .penRequestBatchStudents(batchEntities.get(0).getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList()))
      .students(students).build();
    final PenRequestBatchReportData reportData = reportMapper.toReportData(sagaData);

    assertThat(reportData.getDiffList().size()).isEqualTo(1);
    assertThat(reportData.getDiffList().get(0).getSchool().getBirthDate()).isEqualTo("20111234");
    assertThat(reportData.getDiffList().get(0).getMin().getBirthDate()).isEqualTo("19900734");
  }

  @Test
  public void testToReportUserMatchedListItem_GivenNullStudent_ShouldNotThrowError() throws IOException {
    final var batchEntities = PenRequestBatchTestUtils.createBatchStudents(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_archived_with_pen.json", 1,
      (batch) -> batch.setProcessDate(LocalDateTime.parse("2021-03-23T13:04:48.840098")));

    final var student = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130009").pen("123456780").build();
    final List<Student> students = new ArrayList<>();
    students.add(student);
    final var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder()
      .facsimile("3333333333")
      .telephone("5555555555")
      .fromEmail("test@abc.com")
      .mailingAddress("mailing address")
      .students(students)
      .studentRegistrationContacts(PenCoordinator.builder().penCoordinatorEmail("test@email.com").penCoordinatorName("Joe Blow").build())
      .schoolName("Cataline")
      .penRequestBatch(mapper.toStructure(batchEntities.get(0)))
      .penRequestBatchStudents(batchEntities.get(0).getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList()))
      .penRequestBatchStudentValidationIssues(new HashMap<>())
      .build();
    final PenRequestBatchReportData reportData = reportMapper.toReportData(sagaData);

    assertThat(reportData.getDiffList().size()).isEqualTo(1);
  }

  @Test
  public void testToReportUserMatchedListItem_GivenNullValues_ShouldMapSuccessfully() throws IOException {
    final var batchEntities = PenRequestBatchTestUtils.createBatchStudents(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_null_data.json", 1,
        (batch) -> batch.setProcessDate(LocalDateTime.parse("2021-03-23T13:04:48.840098")));

    final var student1 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130002").pen("123456785").demogCode("C").build();
    final var student2 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130003").pen("123456780").legalLastName("BILL").build();
    final List<Student> students = new ArrayList<>();
    students.add(student1);
    students.add(student2);

    final var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder()
      .facsimile("3333333333")
      .telephone("5555555555")
      .fromEmail("test@abc.com")
      .mailingAddress("mailing address")
      .schoolName("Cataline")
      .penRequestBatch(mapper.toStructure(batchEntities.get(0)))
      .penRequestBatchStudents(batchEntities.get(0).getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList()))
      .penRequestBatchStudentValidationIssues(new HashMap<>())
      .students(students).build();

    final PenRequestBatchReportData reportData = reportMapper.toReportData(sagaData);

    assertThat(reportData.getProcessDate()).isEqualTo("2021/03/23");
    assertThat(reportData.getProcessTime()).isEqualTo("13:04");
    assertThat(reportData.getSubmissionNumber()).isEqualTo(batchEntities.get(0).getSubmissionNumber());
    assertThat(reportData.getReportDate()).isEqualTo("2021-MAR-23");
    assertThat(reportData.getReviewer()).isEqualTo("School PEN Coordinator");
    assertThat(reportData.getMincode()).isEqualTo(formatMincode(batchEntities.get(0).getMincode()));
    assertThat(reportData.getSchoolName()).isEqualTo("Cataline");
    assertThat(reportData.getFascimile()).isEqualTo("3333333333");
    assertThat(reportData.getTelephone()).isEqualTo("5555555555");
    assertThat(reportData.getMailingAddress()).isEqualTo("mailing address");
    assertThat(reportData.getPenCordinatorEmail()).isEqualTo("test@abc.com");

    assertThat(reportData.getDiffList().size()).isEqualTo(1);
    assertThat(reportData.getDiffList().get(0).getMin().getBirthDate())isEmpty();
    assertThat(reportData.getDiffList().get(0).getMin().getGender())isEmpty();
    assertThat(reportData.getDiffList().get(0).getMin().getGivenName())isEmpty();
    assertThat(reportData.getDiffList().get(0).getMin().getLegalMiddleNames())isEmpty();
    assertThat(reportData.getDiffList().get(0).getMin().getPen()).isEqualTo("123456780");
    assertThat(reportData.getDiffList().get(0).getMin().getReason()).isEqualTo(null);
    assertThat(reportData.getDiffList().get(0).getMin().getSchoolID())isEmpty();
    assertThat(reportData.getDiffList().get(0).getMin().getSurname()).isEqualTo("BILL");
    assertThat(reportData.getDiffList().get(0).getMin().getUsualName())isEmpty();

    assertThat(reportData.getDiffList().size()).isEqualTo(1);
    assertThat(reportData.getDiffList().get(0).getSchool().getBirthDate())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getGender())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getGivenName())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getLegalMiddleNames())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getPen())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getReason()).isEqualTo(null);
    assertThat(reportData.getDiffList().get(0).getSchool().getSchoolID())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getSurname())isEmpty();
    assertThat(reportData.getDiffList().get(0).getSchool().getUsualName())isEmpty();

    assertThat(reportData.getSysMatchedList().size()).isEqualTo(2);
    assertThat(reportData.getSysMatchedList().get(1).getBirthDate())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getGender())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getGivenName())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getLegalMiddleNames())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getPen())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getReason()).isEqualTo(null);
    assertThat(reportData.getSysMatchedList().get(1).getSchoolID())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getSurname())isEmpty();
    assertThat(reportData.getSysMatchedList().get(1).getUsualName())isEmpty();

    assertThat(reportData.getNewPenList().size()).isEqualTo(2);
    assertThat(reportData.getPendingList().size()).isEqualTo(4);
    assertThat(reportData.getSysMatchedList().size()).isEqualTo(2);
  }

    public String formatMincode(String mincode) {
      return mincode.substring(0, 3) + " " + mincode.substring(3);
    }
}
