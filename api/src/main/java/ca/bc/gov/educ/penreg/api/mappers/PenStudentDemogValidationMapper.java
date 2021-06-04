package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen student demog validation mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenStudentDemogValidationMapper {
  /**
   * The constant mapper.
   */
  PenStudentDemogValidationMapper mapper = Mappers.getMapper(PenStudentDemogValidationMapper.class);

  /**
   * To student demog validation payload pen request batch student validation payload.
   *
   * @param penRequestBatchStudentSagaData the pen request batch student saga data
   * @return the pen request batch student validation payload
   */
  @Mapping(target = "transactionID", ignore = true)
  @Mapping(target = "submissionNumber", ignore = true)
  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "issueList", ignore = true)
  @Mapping(target = "isInteractive", constant = "false")
  @Mapping(target = "bestMatchPEN", ignore = true)
  PenRequestBatchStudentValidationPayload toStudentDemogValidationPayload(PenRequestBatchStudentSagaData penRequestBatchStudentSagaData);

  /**
   * To validation payload pen request student validation payload.
   *
   * @param request the request
   * @return the pen request student validation payload
   */
  @Mapping(target = "submittedPen", ignore = true)
  @Mapping(target = "submissionNumber", ignore = true)
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "penRequestBatchStudentStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "issueList", ignore = true)
  @Mapping(target = "bestMatchPEN", ignore = true)
  @Mapping(target = "assignedPEN", ignore = true)
  @Mapping(target = "transactionID", expression = "java(java.util.UUID.randomUUID().toString())")
  @Mapping(target = "localID", source = "localStudentID")
  @Mapping(target = "isInteractive", expression = "java(false)")
  @Mapping(target = "usualMiddleNames", source = "usualMiddleName")
  @Mapping(target = "usualLastName", source = "usualSurname")
  @Mapping(target = "usualFirstName", source = "usualGivenName")
  @Mapping(target = "legalMiddleNames", source = "legalMiddleName")
  @Mapping(target = "legalLastName", source = "legalSurname")
  @Mapping(target = "legalFirstName", source = "legalGivenName")
  @Mapping(target = "gradeCode", source = "enrolledGradeCode")
  @Mapping(target = "genderCode", source = "gender")
  @Mapping(target = "dob", source = "birthDate")
  PenRequestBatchStudentValidationPayload toValidationPayload(PenRequest request);
}
