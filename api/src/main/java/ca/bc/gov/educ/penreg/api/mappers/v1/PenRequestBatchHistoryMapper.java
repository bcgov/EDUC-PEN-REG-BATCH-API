package ca.bc.gov.educ.penreg.api.mappers.v1;


import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchHistory;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchHistorySearch;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface PenRequestBatchHistoryMapper {

  /**
   * The constant mapper.
   */
  PenRequestBatchHistoryMapper mapper = Mappers.getMapper(PenRequestBatchHistoryMapper.class);

  /**
   * To structure pen request batch.
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch
   */
  PenRequestBatchHistory toStructure(PenRequestBatchHistoryEntity penRequestBatchEntity);

  /**
   * To structure pen request batch search.
   *
   * @param penRequestBatchHistoryEntity the pen request batch entity
   * @return the pen request batch search
   */
  PenRequestBatchHistorySearch toSearchStructure(PenRequestBatchHistoryEntity penRequestBatchHistoryEntity);

  /**
   * To model pen request batch history entity.
   *
   * @param penRequestBatchHistory the pen request batch
   * @return the pen request batch entity
   */
  @InheritInverseConfiguration
  PenRequestBatchHistoryEntity toModel(PenRequestBatch penRequestBatchHistory);


}
