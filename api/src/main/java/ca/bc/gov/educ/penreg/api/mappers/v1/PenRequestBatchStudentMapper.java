package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch student mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchStudentMapper {

  /**
   * The constant mapper.
   */
  PenRequestBatchStudentMapper mapper = Mappers.getMapper(PenRequestBatchStudentMapper.class);

  /**
   * To structure pen request batch student.
   *
   * @param penRequestBatchStudentEntity the pen request batch student entity
   * @return the pen request batch student
   */
  @Mapping(source = "penRequestBatchStudentEntity.penRequestBatchEntity.penRequestBatchID", target = "penRequestBatchID")
  @Mapping(source = "penRequestBatchStudentEntity.penRequestBatchEntity.minCode", target = "minCode")
  @Mapping(source = "penRequestBatchStudentEntity.penRequestBatchEntity.submissionNumber", target = "submissionNumber")
  PenRequestBatchStudent  toStructure(PenRequestBatchStudentEntity penRequestBatchStudentEntity);

  /**
   * To model pen request batch student entity.
   *
   * @param penRequestBatchStudent the pen request batch student
   * @return the pen request batch student entity
   */
  @InheritInverseConfiguration
  PenRequestBatchStudentEntity toModel(PenRequestBatchStudent penRequestBatchStudent);


  /**
   * To prb student pen request batch student.
   *
   * @param basePenRequestBatchStudentSagaData the base pen request batch student saga data
   * @return the pen request batch student
   */
  @Mapping(target = "submissionNumber", ignore = true)
  @Mapping(target = "repeatRequestSequenceNumber", ignore = true)
  @Mapping(target = "repeatRequestOriginalID", ignore = true)
  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "minCode", ignore = true)
  @Mapping(target = "bestMatchPEN", ignore = true)
  PenRequestBatchStudent toPrbStudent(BasePenRequestBatchStudentSagaData basePenRequestBatchStudentSagaData);

}
