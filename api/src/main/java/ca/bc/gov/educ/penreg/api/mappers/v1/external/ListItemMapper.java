package ca.bc.gov.educ.penreg.api.mappers.v1.external;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.external.ListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {LocalDateTimeMapper.class, UUIDMapper.class})
public interface ListItemMapper {

  ListItemMapper mapper = Mappers.getMapper(ListItemMapper.class);

  @Mapping(target = "validationIssues", ignore = true)
  @Mapping(target = "usualSurname", source = "usualLastName")
  @Mapping(target = "usualGivenName", source = "usualFirstName")
  @Mapping(target = "legalSurname", source = "legalLastName")
  @Mapping(target = "legalGivenName", source = "legalFirstName")
  @Mapping(target = "gender", source = "genderCode")
  @Mapping(target = "enrolledGradeCode", source = "gradeCode")
  @Mapping(target = "birthDate", expression = "java(org.apache.commons.lang3.RegExUtils.removeAll(student.getDob(), \"[^\\\\d]\"))")
  ListItem toListItem(Student student);

  @Mapping(target = "validationIssues", ignore = true)
  @Mapping(target = "usualSurname", source = "usualLastName")
  @Mapping(target = "usualGivenName", source = "usualFirstName")
  @Mapping(target = "pen", source = "assignedPEN")
  @Mapping(target = "mincode", source = "penRequestBatchEntity.mincode")
  @Mapping(target = "legalSurname", source = "legalLastName")
  @Mapping(target = "legalGivenName", source = "legalFirstName")
  @Mapping(target = "gender", source = "genderCode")
  @Mapping(target = "enrolledGradeCode", source = "gradeCode")
  @Mapping(target = "birthDate", source = "dob")
  ListItem toListItem(PenRequestBatchStudentEntity penRequestBatchStudentEntity);

  @Mapping(target = "validationIssues", ignore = true)
  @Mapping(target = "usualSurname", source = "usualLastName")
  @Mapping(target = "usualGivenName", source = "usualFirstName")
  @Mapping(target = "pen", source = "submittedPen")
  @Mapping(target = "mincode", source = "penRequestBatchEntity.mincode")
  @Mapping(target = "legalSurname", source = "legalLastName")
  @Mapping(target = "legalGivenName", source = "legalFirstName")
  @Mapping(target = "gender", source = "genderCode")
  @Mapping(target = "enrolledGradeCode", source = "gradeCode")
  @Mapping(target = "birthDate", source = "dob")
  ListItem toDiffListItem(PenRequestBatchStudentEntity penRequestBatchStudentEntity);
}
