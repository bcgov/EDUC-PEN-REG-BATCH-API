package ca.bc.gov.educ.penreg.api.mappers.v1;


import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchSearch;
import ca.bc.gov.educ.penreg.api.struct.v1.external.PenRequestBatchSubmission;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchMapper {

  /**
   * The constant mapper.
   */
  PenRequestBatchMapper mapper = Mappers.getMapper(PenRequestBatchMapper.class);

  /**
   * To structure pen request batch.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch
   */
  PenRequestBatch toStructure(PenRequestBatchEntity penRequestBatchEntity);

  /**
   * To structure pen request batch search.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch search
   */
  PenRequestBatchSearch toSearchStructure(PenRequestBatchEntity penRequestBatchEntity);

  /**
   * To model pen request batch entity.
   *
   * @param penRequestBatch the pen request batch
   * @return the pen request batch entity
   */
  @InheritInverseConfiguration
  PenRequestBatchEntity toModel(PenRequestBatch penRequestBatch);

  @Mapping(target = "updateDate", ignore = true)
  @Mapping(target = "createDate", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  PenRequestBatchEntity toModel(PenRequestBatchSubmission penRequestBatchSubmission);
}
