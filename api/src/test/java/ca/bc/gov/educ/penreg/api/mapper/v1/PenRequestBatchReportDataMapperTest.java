package ca.bc.gov.educ.penreg.api.mapper.v1;

import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchReportDataMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Slf4j
public class PenRequestBatchReportDataMapperTest {

    @Autowired
    private PenRequestBatchRepository repository;

    private static final PenRequestBatchReportDataMapper reportMapper = PenRequestBatchReportDataMapper.mapper;
    private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
    private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;

    /**
     * teardown
     */
    @After
    public void after() {
        this.repository.deleteAll();
    }

    @Test
    public void testToReportUserMatchedListItem_GivenAllValues_ShouldMapSuccessfully() throws IOException {
        var batchEntities = PenRequestBatchUtils.createBatchStudents(this.repository, "mock_pen_req_batch_repeat.json", "mock_pen_req_batch_student_archived_with_pen.json", 1,
                (batch) -> batch.setProcessDate(LocalDateTime.parse("2021-03-23T13:04:48.840098")));

        var student1 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130002").dob("19900704").genderCode("M").legalFirstName("Mike").pen("123456785").legalLastName("Joe").legalMiddleNames("Tim").usualFirstName("Bob").usualLastName("Smithy").usualMiddleNames("Smalls").mincode(batchEntities.get(0).getMincode()).demogCode("C").build();
        var student2 = Student.builder().studentID("566ee980-8e5f-11eb-8dcd-0242ac130003").dob("19900703").genderCode("F").legalFirstName("Ted").pen("123456780").legalLastName("Jones").legalMiddleNames("Jim").usualFirstName("Steal").usualLastName("Mr").usualMiddleNames("Yo Girl").mincode(batchEntities.get(0).getMincode()).demogCode("A").build();
        List<Student> students = new ArrayList<>();
        students.add(student1);
        students.add(student2);

        var sagaData = PenRequestBatchArchiveAndReturnSagaData.builder().facsimile("3333333333").telephone("5555555555").fromEmail("test@abc.com").mailingAddress("mailing address").penCordinatorEmail("test@email.com").schoolName("Cataline").penRequestBatch(mapper.toStructure(batchEntities.get(0))).penRequestBatchStudents(batchEntities.get(0).getPenRequestBatchStudentEntities().stream().map(studentMapper::toStructure).collect(Collectors.toList())).matchedStudents(students).build();

        PenRequestBatchReportData reportData = reportMapper.toReportData(sagaData);

        assertThat(reportData.getProcessDate()).isEqualTo("2021-03-23");
        assertThat(reportData.getProcessTime()).isEqualTo("13:04:48");
        assertThat(reportData.getSubmissionNumber()).isEqualTo(batchEntities.get(0).getSubmissionNumber());
        assertThat(reportData.getReportDate()).isEqualTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MMM-dd")).toUpperCase().replace(".", ""));
        assertThat(reportData.getReviewer()).isEqualTo(batchEntities.get(0).getUpdateUser());
        assertThat(reportData.getMincode()).isEqualTo(batchEntities.get(0).getMincode());
        assertThat(reportData.getSchoolName()).isEqualTo("Cataline");
        assertThat(reportData.getFascimile()).isEqualTo("3333333333");
        assertThat(reportData.getTelephone()).isEqualTo("5555555555");
        assertThat(reportData.getMailingAddress()).isEqualTo("mailing address");
        assertThat(reportData.getPenCordinatorEmail()).isEqualTo("test@abc.com");

        assertThat(reportData.getDiffList().size()).isEqualTo(1);
        assertThat(reportData.getDiffList().get(0).getMin().getBirthDate()).isEqualTo("19900703");
        assertThat(reportData.getDiffList().get(0).getMin().getGender()).isEqualTo("F");
        assertThat(reportData.getDiffList().get(0).getMin().getGivenName()).isEqualTo("Ted");
        assertThat(reportData.getDiffList().get(0).getMin().getLegalMiddleNames()).isEqualTo("Jim");
        assertThat(reportData.getDiffList().get(0).getMin().getPen()).isEqualTo("123456780");
        assertThat(reportData.getDiffList().get(0).getMin().getReason()).isEqualTo(null);
        assertThat(reportData.getDiffList().get(0).getMin().getSchoolID()).isEqualTo(batchEntities.get(0).getMincode());
        assertThat(reportData.getDiffList().get(0).getMin().getSurname()).isEqualTo("Jones");
        assertThat(reportData.getDiffList().get(0).getMin().getUsualName()).isEqualTo("Mr, Steal, Yo Girl");

        assertThat(reportData.getDiffList().size()).isEqualTo(1);
        assertThat(reportData.getDiffList().get(0).getSchool().getBirthDate()).isEqualTo("20111208");
        assertThat(reportData.getDiffList().get(0).getSchool().getGender()).isEqualTo("M");
        assertThat(reportData.getDiffList().get(0).getSchool().getGivenName()).isEqualTo("BOY");
        assertThat(reportData.getDiffList().get(0).getSchool().getLegalMiddleNames()).isEqualTo("JAMIESON");
        assertThat(reportData.getDiffList().get(0).getSchool().getPen()).isEqualTo("123456780");
        assertThat(reportData.getDiffList().get(0).getSchool().getReason()).isEqualTo("Here's some more info");
        assertThat(reportData.getDiffList().get(0).getSchool().getSchoolID()).isEqualTo(batchEntities.get(0).getMincode());
        assertThat(reportData.getDiffList().get(0).getSchool().getSurname()).isEqualTo("BRODY");
        assertThat(reportData.getDiffList().get(0).getSchool().getUsualName()).isEqualTo("JOSEPH, BRAYDON, SMIT");

        assertThat(reportData.getConfirmedList().size()).isEqualTo(1);
        assertThat(reportData.getConfirmedList().get(0).getMin().getBirthDate()).isEqualTo("19900704");
        assertThat(reportData.getConfirmedList().get(0).getMin().getGender()).isEqualTo("M");
        assertThat(reportData.getConfirmedList().get(0).getMin().getGivenName()).isEqualTo("Mike");
        assertThat(reportData.getConfirmedList().get(0).getMin().getLegalMiddleNames()).isEqualTo("Tim");
        assertThat(reportData.getConfirmedList().get(0).getMin().getPen()).isEqualTo("123456785");
        assertThat(reportData.getConfirmedList().get(0).getMin().getReason()).isEqualTo(null);
        assertThat(reportData.getConfirmedList().get(0).getMin().getSchoolID()).isEqualTo(batchEntities.get(0).getMincode());
        assertThat(reportData.getConfirmedList().get(0).getMin().getSurname()).isEqualTo("Joe");
        assertThat(reportData.getConfirmedList().get(0).getMin().getUsualName()).isEqualTo("Smithy, Bob, Smalls");

        assertThat(reportData.getConfirmedList().size()).isEqualTo(1);
        assertThat(reportData.getConfirmedList().get(0).getSchool().getBirthDate()).isEqualTo("20111208");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getGender()).isEqualTo("M");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getGivenName()).isEqualTo("BRAYDON");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getLegalMiddleNames()).isEqualTo("JAMIESON");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getPen()).isEqualTo("123456785");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getReason()).isEqualTo("Here's some info");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getSchoolID()).isEqualTo(batchEntities.get(0).getMincode());
        assertThat(reportData.getConfirmedList().get(0).getSchool().getSurname()).isEqualTo("JOSEPH");
        assertThat(reportData.getConfirmedList().get(0).getSchool().getUsualName()).isEqualTo("JOSEPH, BRAYDON, KIM");

        assertThat(reportData.getNewPenList().size()).isEqualTo(2);
        assertThat(reportData.getPendingList().size()).isEqualTo(5);
        assertThat(reportData.getSysMatchedList().size()).isEqualTo(1);
    }
}