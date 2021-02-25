package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentStatusCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * The interface Student status code mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface StudentStatusCodeMapper {
  /**
   * The constant mapper.
   */
  StudentStatusCodeMapper mapper = Mappers.getMapper(StudentStatusCodeMapper.class);

  /**
   * To struct pen request batch student status code.
   *
   * @param entity the entity
   * @return the pen request batch student status code
   */
  PenRequestBatchStudentStatusCode toStruct(PenRequestBatchStudentStatusCodeEntity entity);
}
