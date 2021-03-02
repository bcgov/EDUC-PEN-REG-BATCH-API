package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PENWebBlob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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
  @Mapping(target = "fileContents", source="penWebBlobEntity.fileContents", qualifiedByName = "blobToString")
  PENWebBlob toStructure(PENWebBlobEntity penWebBlobEntity);

  @Named("blobToString")
  default String getFileContentsAsFormattedString(byte[] fileContents) {
    StringBuffer buf = new StringBuffer();
    try {
      InputStreamReader inStreamReader = new InputStreamReader(new ByteArrayInputStream(fileContents));
      BufferedReader reader = new BufferedReader(inStreamReader);

      String s = "";
      while ((s = reader.readLine()) != null) {
        buf.append(s);
        buf.append(System.lineSeparator());
      }
    } catch (IOException ioe) {
      return null;
    }
    return buf.toString();
  }
}
