package ca.bc.gov.educ.penreg.api.mappers;

import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudent;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchStudentMapper {

  PenRequestBatchStudentMapper mapper = Mappers.getMapper(PenRequestBatchStudentMapper.class);

  @Mapping(source = "penRequestBatchStudentEntity.penRequestBatchEntity.penRequestBatchID", target = "penRequestBatchID")
  PenRequestBatchStudent  toStructure(PenRequestBatchStudentEntity penRequestBatchStudentEntity);
  @InheritInverseConfiguration
  PenRequestBatchStudentEntity toModel(PenRequestBatchStudent penRequestBatchStudent);

}
