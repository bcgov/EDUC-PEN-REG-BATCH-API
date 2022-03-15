package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestValidationIssue;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentValidationIssue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch student validation issue mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchStudentValidationIssueMapper {
  /**
   * The constant mapper.
   */
  PenRequestBatchStudentValidationIssueMapper mapper = Mappers.getMapper(PenRequestBatchStudentValidationIssueMapper.class);

  /**
   * To model pen request batch student validation issue entity.
   *
   * @param penRequestValidationIssue the pen request batch student validation issue
   * @return the pen request batch student validation issue entity
   */
  @Mapping(target = "penRequestBatchStudentEntity", ignore = true)
  PenRequestBatchStudentValidationIssueEntity toModel(PenRequestValidationIssue penRequestValidationIssue);

  @Mapping(target = "penRequestBatchStudentValidationIssueId", ignore = true)
  PenRequestValidationIssue toStruct(PenRequestBatchStudentValidationIssueEntity entity);

  @Mapping(target = "penRequestBatchStudentValidationIssueId", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", source = "entity.penRequestBatchStudentEntity.penRequestBatchStudentID")
  PenRequestBatchStudentValidationIssue toPenRequestBatchStruct(PenRequestBatchStudentValidationIssueEntity entity);
}
