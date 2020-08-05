package ca.bc.gov.educ.penreg.api.mappers.v1;


import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
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
   * To model pen request batch entity.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch entity
   */
  @InheritInverseConfiguration
  PenRequestBatchEntity toModel(PenRequestBatch penRequestBatch);


}
