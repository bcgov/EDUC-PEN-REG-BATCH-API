package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch student saga data mapper.
 */
@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchStudentSagaDataMapper {
  /**
   * The constant mapper.
   */
  PenRequestBatchStudentSagaDataMapper mapper = Mappers.getMapper(PenRequestBatchStudentSagaDataMapper.class);

  /**
   * To pen req batch student saga data pen request batch student saga data.
   *
   * @param entity the entity
   * @return the pen request batch student saga data
   */
  @Mapping(target = "penMatchResult", ignore = true)
  @Mapping(target = "mincode", ignore = true)
  @Mapping(target = "penRequestBatchID", source = "entity.penRequestBatchEntity.penRequestBatchID")
  PenRequestBatchStudentSagaData toPenReqBatchStudentSagaData(PenRequestBatchStudentEntity entity);
}