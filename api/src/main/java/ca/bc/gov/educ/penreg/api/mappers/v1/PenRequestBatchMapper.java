package ca.bc.gov.educ.penreg.api.mappers.v1;


import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.PenMatchStudent;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchSearch;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmission;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestResult;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchMapper {

  /**
   * The constant mapper.
   */
  PenRequestBatchMapper mapper = Mappers.getMapper(PenRequestBatchMapper.class);

  /**
   * To structure pen request batch.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch
   */
  PenRequestBatch toStructure(PenRequestBatchEntity penRequestBatchEntity);

  /**
   * To structure pen request batch search.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch search
   */
  PenRequestBatchSearch toSearchStructure(PenRequestBatchEntity penRequestBatchEntity);

  /**
   * To model pen request batch entity.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch entity
   */
  @InheritInverseConfiguration
  PenRequestBatchEntity toModel(PenRequestBatch penRequestBatch);

  @Mapping(target = "updateDate", ignore = true)
  @Mapping(target = "createDate", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  PenRequestBatchEntity toModel(PenRequestBatchSubmission penRequestBatchSubmission);

  @Mapping(target = "pen", ignore = true)
  @Mapping(target = "validationIssues", ignore = true)
  PenRequestResult toPenRequestResult(PenRequest penRequest);

  @Mapping(target = "usualSurname", source = "usualLastName")
  @Mapping(target = "usualMiddleName", source = "usualMiddleNames")
  @Mapping(target = "usualGivenName", source = "usualFirstName")
  @Mapping(target = "localStudentID", source = "localID")
  @Mapping(target = "legalSurname", source = "legalLastName")
  @Mapping(target = "legalMiddleName", source = "legalMiddleNames")
  @Mapping(target = "legalGivenName", source = "legalFirstName")
  @Mapping(target = "gender", source = "genderCode")
  @Mapping(target = "enrolledGradeCode", source = "gradeCode")
  @Mapping(target = "birthDate", source = "dob")
  @Mapping(target = "validationIssues", ignore = true)
  PenRequestResult toPenRequestResult(Student student);

  @Mapping(target = "pen", ignore = true)
  @Mapping(target = "surname", source = "legalSurname")
  @Mapping(target = "sex", source = "gender")
  @Mapping(target = "postal", source = "postalCode")
  @Mapping(target = "middleName", source = "legalMiddleName")
  @Mapping(target = "givenName", source = "legalGivenName")
  @Mapping(target = "localID", source = "localStudentID")
  @Mapping(target = "dob", source = "birthDate")
  PenMatchStudent toPenMatch(PenRequest penRequest);

  /**
   * To student student.
   *
   * @param request the request
   * @param pen     the pen
   * @return the student
   */
  @Mapping(target = "usualMiddleNames", source = "request.usualMiddleName")
  @Mapping(target = "usualLastName", source = "request.usualSurname")
  @Mapping(target = "usualFirstName", source = "request.usualGivenName")
  @Mapping(target = "trueStudentID", ignore = true)
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "statusCode", constant = "A")
  @Mapping(target = "sexCode", expression = "java(ca.bc.gov.educ.penreg.api.util.CodeUtil.getSexCodeFromGenderCode(request.getGender()))")
  @Mapping(target = "memo", ignore = true)
  @Mapping(target = "localID", expression = "java(ca.bc.gov.educ.penreg.api.util.LocalIDUtil.changeBadLocalID(request.getLocalStudentID()))")
  @Mapping(target = "legalMiddleNames", source = "request.legalMiddleName")
  @Mapping(target = "legalLastName", source = "request.legalSurname")
  @Mapping(target = "legalFirstName", source = "request.legalGivenName")
  @Mapping(target = "historyActivityCode", constant = "REQNEW")
  @Mapping(target = "gradeYear", ignore = true)
  @Mapping(target = "gradeCode", source = "request.enrolledGradeCode")
  @Mapping(target = "genderCode", source = "request.gender")
  @Mapping(target = "emailVerified", constant = "N")
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "dob", expression = "java(ca.bc.gov.educ.penreg.api.util.LocalDateTimeUtil.getAPIFormattedDateOfBirth(request.getBirthDate()))")
  @Mapping(target = "demogCode", constant = "A")
  @Mapping(target = "deceasedDate", ignore = true)
  Student toStudent(PenRequest request, String pen);
}
