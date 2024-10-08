package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.model.v1.SagaEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface SagaMapper {
  /**
   * The constant mapper.
   */
  SagaMapper mapper = Mappers.getMapper(SagaMapper.class);

  /**
   * To struct ca . bc . gov . educ . penreg . api . struct . v 1 . saga.
   *
   * @param entity the entity
   * @return the ca . bc . gov . educ . penreg . api . struct . v 1 . saga
   */
  ca.bc.gov.educ.penreg.api.struct.v1.Saga toStruct(Saga entity);

  /**
   * To model saga.
   *
   * @param struct the struct
   * @return the saga
   */
  Saga toModel(ca.bc.gov.educ.penreg.api.struct.v1.Saga struct);

  @Mapping(target = "sagaId", source = "saga.sagaId")
  ca.bc.gov.educ.penreg.api.struct.v1.SagaEvent toEventStruct(SagaEvent sagaEvent);
}
