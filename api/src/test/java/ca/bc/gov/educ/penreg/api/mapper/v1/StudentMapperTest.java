package ca.bc.gov.educ.penreg.api.mapper.v1;

import static org.assertj.core.api.Assertions.assertThat;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.mappers.StudentMapper;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;

public class StudentMapperTest extends BasePenRegAPITest {

    private static final StudentMapper studentMapper = StudentMapper.mapper;


    @Test
    public void testToStudent_GivenBadLocalID_ShouldChangeToNull() throws IOException {

        // created a student with NA
        final var localIdNA = BasePenRequestBatchStudentSagaData.builder()
                .studentID("566ee980-8e5f-11eb-8dcd-0242ac130003")
                .genderCode("F")
                .legalFirstName("Ted")
                .localID("NA")
                .penRequestBatchStudentID(UUID.randomUUID())
                .legalLastName("Liu")
                .legalMiddleNames("Jim")
                .usualFirstName("Steal")
                .usualLastName("Mr")
                .mincode("345678")
                .build();

        // created a student with N#A
        final var localIdNHashtagA = BasePenRequestBatchStudentSagaData.builder()
                .studentID("566ee980-8e5f-11eb-8dcd-0242ac130004")
                .genderCode("M")
                .legalFirstName("Carry")
                .localID("N#A")
                .penRequestBatchStudentID(UUID.randomUUID())
                .legalLastName("Ms")
                .legalMiddleNames("Jim")
                .usualFirstName("Steal")
                .usualLastName("Mr")
                .mincode("234567")
                .build();

        // created a student with N/A
        final var localIdNForwardSlashA = BasePenRequestBatchStudentSagaData.builder()
                .studentID("566ee980-8e5f-11eb-8dcd-0242ac160003")
                .genderCode("M")
                .legalFirstName("Eden")
                .localID("N/A")
                .penRequestBatchStudentID(UUID.randomUUID())
                .legalLastName("Liu")
                .legalMiddleNames("Ben")
                .usualFirstName("ET")
                .usualLastName("Mr")
                .mincode("123456")
                .build();

        // created a valid student localID
        final var localIdValid = BasePenRequestBatchStudentSagaData.builder()
                .studentID("566ee980-8e5f-11eb-8dcd-0242ac160003")
                .genderCode("M")
                .legalFirstName("Adam")
                .localID("1488645")
                .penRequestBatchStudentID(UUID.randomUUID())
                .legalLastName("Swaski")
                .mincode("123456")
                .build();

        // pass the student to mapper
        final var localIdNAData = studentMapper.toStudent(localIdNA);
        final var localIdNHashtagAData = studentMapper.toStudent(localIdNHashtagA);
        final var localIdNAForwardSlashAData = studentMapper.toStudent(localIdNForwardSlashA);
        final var localIdValidData = studentMapper.toStudent(localIdValid);

        // check that bad localIDs are changed to null
        assertThat(localIdNAData.getLocalID() == null).isEqualTo(true);
        assertThat(localIdNHashtagAData.getLocalID() == null).isEqualTo(true);
        assertThat(localIdNAForwardSlashAData.getLocalID() == null).isEqualTo(true);

        // valid localID should not be null
        assertThat(localIdValidData.getLocalID() == null).isEqualTo(false);

    }

}
