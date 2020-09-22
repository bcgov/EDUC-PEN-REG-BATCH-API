package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.struct.PenMatchStudent;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen match saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenMatchSagaMapper {
  /**
   * The constant mapper.
   */
  PenMatchSagaMapper mapper = Mappers.getMapper(PenMatchSagaMapper.class);

  /**
   * To pen match student pen match student.
   *
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @return the pen match student
   */
  @Mapping(target = "usualSurname", source = "usualLastName")
  @Mapping(target = "usualMiddleName", source = "usualMiddleNames")
  @Mapping(target = "usualGivenName", source = "usualFirstName")
  @Mapping(target = "surname", source = "legalLastName")
  @Mapping(target = "sex", source = "genderCode")
  @Mapping(target = "postal", source = "postalCode")
  @Mapping(target = "pen", source = "submittedPen")
  @Mapping(target = "mincode", source = "mincode")
  @Mapping(target = "middleName", source = "legalMiddleNames")
  @Mapping(target = "givenName", source = "legalFirstName")
  @Mapping(target = "enrolledGradeCode", source = "gradeCode")
  PenMatchStudent toPenMatchStudent(PenRequestBatchStudentSagaData penRequestBatchStudentSagaData);
}
