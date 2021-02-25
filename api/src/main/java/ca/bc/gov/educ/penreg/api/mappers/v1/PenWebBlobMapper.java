package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PENWebBlob;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen web blob mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenWebBlobMapper {
  /**
   * The constant mapper.
   */
  PenWebBlobMapper mapper = Mappers.getMapper(PenWebBlobMapper.class);

  /**
   * To model pen web blob entity.
   *
   * @param penWebBlob the pen web blob
   * @return the pen web blob entity
   */
  @Mapping(target = "fileContents", ignore = true)
  PENWebBlobEntity toModel(PENWebBlob penWebBlob);

  /**
   * To structure pen web blob.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @return the pen web blob
   */
  @InheritInverseConfiguration
  PENWebBlob toStructure(PENWebBlobEntity penWebBlobEntity);
}
