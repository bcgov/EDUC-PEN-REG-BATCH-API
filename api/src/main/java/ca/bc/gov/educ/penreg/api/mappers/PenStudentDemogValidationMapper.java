package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentValidationPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen student demog validation mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
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
  @Mapping(target = "minCode", ignore = true)
  @Mapping(target = "issueList", ignore = true)
  @Mapping(target = "isInteractive", constant = "false")
  @Mapping(target = "bestMatchPEN", ignore = true)
  PenRequestBatchStudentValidationPayload toStudentDemogValidationPayload(PenRequestBatchStudentSagaData penRequestBatchStudentSagaData);

}
