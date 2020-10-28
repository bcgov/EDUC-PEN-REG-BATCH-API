package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.StudentTwinAssociation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Student mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface StudentMapper {
  /**
   * The constant mapper.
   */
  StudentMapper mapper = Mappers.getMapper(StudentMapper.class);

  /**
   * To student student.
   *
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @return the student
   */
  @Mapping(target = "usualMiddleNames", source="penRequestBatchStudentSagaData.usualMiddleNames")
  @Mapping(target = "usualLastName", source="penRequestBatchStudentSagaData.usualLastName")
  @Mapping(target = "usualFirstName", source="penRequestBatchStudentSagaData.usualFirstName")
  @Mapping(target = "updateUser", source="penRequestBatchStudentSagaData.updateUser")
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "statusCode", constant="A")
  @Mapping(target = "sexCode", expression="java(ca.bc.gov.educ.penreg.api.util.CodeUtil.getSexCodeFromGenderCode(penRequestBatchStudentSagaData.getGenderCode()))")
  @Mapping(target = "postalCode", source="penRequestBatchStudentSagaData.postalCode")
  @Mapping(target = "pen", ignore = true)
  @Mapping(target = "mincode", source="penRequestBatchStudentSagaData.mincode")
  @Mapping(target = "memo", ignore = true)
  @Mapping(target = "localID", source="penRequestBatchStudentSagaData.localID")
  @Mapping(target = "legalMiddleNames", source="penRequestBatchStudentSagaData.legalMiddleNames")
  @Mapping(target = "legalLastName", source="penRequestBatchStudentSagaData.legalLastName")
  @Mapping(target = "legalFirstName", source="penRequestBatchStudentSagaData.legalFirstName")
  @Mapping(target = "gradeYear", ignore = true)
  @Mapping(target = "gradeCode", source="penRequestBatchStudentSagaData.gradeCode")
  @Mapping(target = "genderCode", source="penRequestBatchStudentSagaData.genderCode")
  @Mapping(target = "emailVerified", constant="N")
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "dob", expression="java(ca.bc.gov.educ.penreg.api.util.LocalDateTimeUtil.getAPIFormattedDateOfBirth(penRequestBatchStudentSagaData.getDob()))")
  @Mapping(target = "demogCode", ignore = true)
  @Mapping(target = "deceasedDate",ignore = true)
  @Mapping(target = "createUser", source="penRequestBatchStudentSagaData.createUser")
  Student toStudent(BasePenRequestBatchStudentSagaData penRequestBatchStudentSagaData);
}
