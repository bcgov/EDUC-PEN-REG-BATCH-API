package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {StringMapper.class})
public interface PenCoordinatorMapper {
  PenCoordinatorMapper mapper = Mappers.getMapper(PenCoordinatorMapper.class);

  PenCoordinator toTrimmedPenCoordinator(PenCoordinator penCoordinator);
}
