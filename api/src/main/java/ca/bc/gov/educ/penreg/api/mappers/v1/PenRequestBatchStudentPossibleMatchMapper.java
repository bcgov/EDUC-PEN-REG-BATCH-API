package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentPossibleMatchEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentPossibleMatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Student status code mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchStudentPossibleMatchMapper {
  /**
   * The constant mapper.
   */
  PenRequestBatchStudentPossibleMatchMapper mapper = Mappers.getMapper(PenRequestBatchStudentPossibleMatchMapper.class);

  @Mapping(target = "penRequestBatchStudentEntity", ignore = true)
  PenRequestBatchStudentPossibleMatchEntity toModel(PenRequestBatchStudentPossibleMatch struct);

  @Mapping(target = "penRequestBatchStudentId", source = "penRequestBatchStudentEntity.penRequestBatchStudentID")
  PenRequestBatchStudentPossibleMatch toStruct(PenRequestBatchStudentPossibleMatchEntity entity);
}

