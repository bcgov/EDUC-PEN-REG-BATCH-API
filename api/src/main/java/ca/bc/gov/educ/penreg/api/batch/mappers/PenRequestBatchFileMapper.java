package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch file mapper.
 */
@Mapper
@DecoratedWith(PenRequestBatchFileDecorator.class)
public interface PenRequestBatchFileMapper {
  /**
   * The constant mapper.
   */
  PenRequestBatchFileMapper mapper = Mappers.getMapper(PenRequestBatchFileMapper.class);
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";


  /**
   * To pen req batch entity loaded pen request batch entity.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @param file             the file
   * @return the pen request batch entity
   */
  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  @Mapping(target = "ministryPRBSourceCode", ignore = true)
  @Mapping(target = "extractDate", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "schoolGroupCode", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "newPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getSubmissionNumber() ))", target = "submissionNumber")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getSourceApplication() ))", target = "sourceApplication")
  @Mapping(source = "penWebBlobEntity.insertDateTime", target = "insertDate")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getFileType() ))", target = "fileType")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getFileName() ))", target = "fileName")
  @Mapping(source = "penWebBlobEntity.studentCount", target = "sourceStudentCount")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getMincode() ))", target = "mincode")
  @Mapping(target = "penRequestBatchProcessTypeCode", expression = "java( ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode())")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getProductID() ))", target = "sisProductID")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getProductName() ))", target = "sisProductName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getVendorName() ))", target = "sisVendorName", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  PenRequestBatchEntity toPenReqBatchEntityLoaded(PENWebBlobEntity penWebBlobEntity, BatchFile file);


  /**
   * To pen request batch student entity pen request batch student entity.
   *
   * @param studentDetails        the student details
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch student entity
   */
  @Mapping(target = "repeatRequestSequenceNumber", ignore = true)
  @Mapping(target = "repeatRequestOriginalID", ignore = true)
  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "questionableMatchStudentId", ignore = true)
  @Mapping(target = "penRequestBatchStudentValidationIssueEntities", ignore = true)
  @Mapping(target = "matchAlgorithmStatusCode", ignore = true)
  @Mapping(target = "infoRequest", ignore = true)
  @Mapping(target = "bestMatchPEN", ignore = true)
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "penRequestBatchStudentStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", ignore = true)
  @Mapping(target = "penRequestBatchEntity", ignore = true)
  @Mapping(target = "assignedPEN", ignore = true)
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(StudentDetails studentDetails, PenRequestBatchEntity penRequestBatchEntity);


  /**
   * To pen req batch entity for business exception pen request batch entity.
   *
   * @param penWebBlobEntity          the pen web blob entity
   * @param reason                    the reason
   * @param penRequestBatchStatusCode the pen request batch status code
   * @param batchFile                 the batch file
   * @param persistStudentRecords     the persist student records
   * @return the pen request batch entity
   */
  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "mincode", ignore = true)
  @Mapping(target = "sisVendorName", ignore = true)
  @Mapping(target = "sisProductName", ignore = true)
  @Mapping(target = "sisProductID", ignore = true)
  @Mapping(target = "schoolName", ignore = true)
  @Mapping(target = "schoolGroupCode", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "officeNumber", ignore = true)
  @Mapping(target = "ministryPRBSourceCode", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "newPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "extractDate", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "contactName", ignore = true)
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getSubmissionNumber() ))", target = "submissionNumber")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getSourceApplication() ))", target = "sourceApplication")
  @Mapping(source = "penWebBlobEntity.insertDateTime", target = "insertDate")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getFileType() ))", target = "fileType")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(penWebBlobEntity.getFileName() ))", target = "fileName")
  @Mapping(source = "penWebBlobEntity.studentCount", target = "sourceStudentCount")
  @Mapping(target = "penRequestBatchProcessTypeCode", expression = "java( ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode())")
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate", expression = "java(java.time.LocalDateTime.now() )")
  PenRequestBatchEntity toPenReqBatchEntityForBusinessException(PENWebBlobEntity penWebBlobEntity, String reason, PenRequestBatchStatusCodes penRequestBatchStatusCode, BatchFile batchFile, boolean persistStudentRecords);

}
