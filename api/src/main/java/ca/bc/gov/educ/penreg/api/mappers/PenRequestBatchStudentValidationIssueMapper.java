package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch student validation issue mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchStudentValidationIssueMapper {
  /**
   * The constant mapper.
   */
  PenRequestBatchStudentValidationIssueMapper mapper = Mappers.getMapper(PenRequestBatchStudentValidationIssueMapper.class);

  /**
   * To model pen request batch student validation issue entity.
   *
   * @param penRequestBatchStudentValidationIssue the pen request batch student validation issue
   * @return the pen request batch student validation issue entity
   */
  @Mapping(target = "penRequestBatchStudentEntity", ignore = true)
  PenRequestBatchStudentValidationIssueEntity toModel(PenRequestBatchStudentValidationIssue penRequestBatchStudentValidationIssue);
}
