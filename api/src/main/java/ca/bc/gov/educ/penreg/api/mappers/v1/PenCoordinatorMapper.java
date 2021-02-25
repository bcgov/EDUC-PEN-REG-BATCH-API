package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.PenCoordinator;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PenCoordinatorMapper {
  PenCoordinatorMapper mapper = Mappers.getMapper(PenCoordinatorMapper.class);

  @Mapping(target = "schoolNumber", source = "mincode.schoolNumber")
  @Mapping(target = "districtNumber", source = "mincode.districtNumber")
  PenCoordinator toStruct(ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator penCoordinator);

  @InheritInverseConfiguration
  ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator toModel(PenCoordinator penCoordinator);
}
