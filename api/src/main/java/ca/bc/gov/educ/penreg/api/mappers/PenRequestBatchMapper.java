package ca.bc.gov.educ.penreg.api.mappers;


import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchMapper {

  PenRequestBatchMapper mapper = Mappers.getMapper(PenRequestBatchMapper.class);

  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  PenRequestBatchEntity toModel(PenRequestBatch penRequestBatch);

  PenRequestBatch toStructure(PenRequestBatchEntity penRequestBatchEntity);


}
