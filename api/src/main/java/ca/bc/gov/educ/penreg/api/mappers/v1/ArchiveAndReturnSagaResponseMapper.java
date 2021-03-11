package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.ArchiveAndReturnSagaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ArchiveAndReturnSagaResponseMapper {
    ArchiveAndReturnSagaResponseMapper mapper = Mappers.getMapper(ArchiveAndReturnSagaResponseMapper.class);

    ArchiveAndReturnSagaResponse toStruct(ca.bc.gov.educ.penreg.api.model.v1.Saga saga);
}
