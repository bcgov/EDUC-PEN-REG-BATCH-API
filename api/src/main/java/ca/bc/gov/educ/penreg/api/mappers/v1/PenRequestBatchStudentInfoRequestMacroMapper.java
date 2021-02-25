package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentInfoRequestMacroEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchStudentInfoRequestMacroMapper {
    PenRequestBatchStudentInfoRequestMacroMapper mapper = Mappers.getMapper(PenRequestBatchStudentInfoRequestMacroMapper.class);

    PenRequestBatchStudentInfoRequestMacro toStructure(PenRequestBatchStudentInfoRequestMacroEntity entity);

    PenRequestBatchStudentInfoRequestMacroEntity toModel(PenRequestBatchStudentInfoRequestMacro struct);
}
