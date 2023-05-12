package ca.bc.gov.educ.penreg.api.mappers.v1.external;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmissionResult;
import java.util.Map;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(PenRequestBatchResultDataDecorator.class)
public interface PenRequestBatchResultDataMapper {

  PenRequestBatchResultDataMapper mapper = Mappers.getMapper(PenRequestBatchResultDataMapper.class);

  @Mapping(target = "pendingList", ignore = true)
  @Mapping(target = "newPenAssignedList", ignore = true)
  @Mapping(target = "exactMatchList", ignore = true)
  @Mapping(target = "differencesList", ignore = true)
  @Mapping(target = "confirmedList", ignore = true)
  PenRequestBatchSubmissionResult toResult(PenRequestBatchEntity penRequestBatch, Map<String, Student> studentMap);
}
