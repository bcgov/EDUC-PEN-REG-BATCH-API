package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchFileMapper {
  PenRequestBatchFileMapper mapper = Mappers.getMapper(PenRequestBatchFileMapper.class);

  @Mapping(target = "unarchivedFlag", ignore = true)
  @Mapping(target = "unarchivedBatchChangedFlag", ignore = true)
  @Mapping(target = "tswAccount", ignore = true)
  @Mapping(target = "submissionNumber", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "sourceStudentCount", ignore = true)
  @Mapping(target = "sourceApplication", ignore = true)
  @Mapping(target = "sisVendorName", ignore = true)
  @Mapping(target = "sisProductName", ignore = true)
  @Mapping(target = "sisProductID", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchSourceCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "officeNumber", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "issuedPenCount", ignore = true)
  @Mapping(target = "insertDate", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "fileType", ignore = true)
  @Mapping(target = "fileName", ignore = true)
  @Mapping(target = "extractDate", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "contactName", ignore = true)
  @Mapping(source = "file.batchFileHeader.minCode", target = "minCode")
  @Mapping(source = "file.batchFileHeader.schoolName", target = "schoolName")
  @Mapping(source = "file.batchFileHeader.emailID", target = "email")
  PenRequestBatchEntity toPenReqBatchEntity(BatchFile file);
}
