package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationIssue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchStudentValidationIssueMapper {
  PenRequestBatchStudentValidationIssueMapper mapper = Mappers.getMapper(PenRequestBatchStudentValidationIssueMapper.class);

  @Mapping(target = "penRequestBatchStudentEntity", ignore = true)
  PenRequestBatchStudentValidationIssueEntity toModel(PenRequestBatchStudentValidationIssue penRequestBatchStudentValidationIssue);
}
