package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
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
@SuppressWarnings("squid:S1214")
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
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileHeader().getMincode() ))", target = "mincode")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileHeader().getSchoolName() ))", target = "schoolName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileHeader().getEmailID() ))", target = "email")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileHeader().getContactName() ))", target = "contactName")
  @Mapping(source = "file.batchFileHeader.officeNumber", target = "officeNumber")
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
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "penRequestBatchStudentStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", ignore = true)
  @Mapping(target = "penRequestBatchEntity", ignore = true)
  @Mapping(target = "assignedPEN", ignore = true)
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getPostalCode()))", target = "postalCode")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getGender()))", target = "genderCode")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getBirthDate()))", target = "dob")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getEnrolledGradeCode()))", target = "gradeCode")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getLegalGivenName()))", target = "legalFirstName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getLegalMiddleName()))", target = "legalMiddleNames")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getLegalSurname()))", target = "legalLastName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getUsualGivenName()))", target = "usualFirstName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getUsualMiddleName()))", target = "usualMiddleNames")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getUsualSurname()))", target = "usualLastName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getLocalStudentID()))", target = "localID")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(studentDetails.getPen()))", target = "submittedPen")
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(StudentDetails studentDetails, PenRequestBatchEntity penRequestBatchEntity);


  /**
   * To pen req batch entity load fail pen request batch entity.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @param reason           the reason
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
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  PenRequestBatchEntity toPenReqBatchEntityLoadFail(PENWebBlobEntity penWebBlobEntity, String reason);

  /**
   * To pen req batch entity load held for size pen request batch entity.
   *
   * @param penWebBlobEntity the pen web blob entity
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
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  PenRequestBatchEntity toPenReqBatchEntityLoadHeldForSize(PENWebBlobEntity penWebBlobEntity, BatchFile file);
}
