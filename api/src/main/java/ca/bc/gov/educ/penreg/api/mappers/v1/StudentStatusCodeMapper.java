package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentStatusCodeEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentStatusCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface StudentStatusCodeMapper {
  StudentStatusCodeMapper mapper = Mappers.getMapper(StudentStatusCodeMapper.class);

  PenRequestBatchStudentStatusCode toStruct(PenRequestBatchStudentStatusCodeEntity entity);
}
