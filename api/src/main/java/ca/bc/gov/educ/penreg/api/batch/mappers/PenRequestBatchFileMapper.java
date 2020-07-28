package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.input.TraxStudentWeb;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.penreg.api.mappers.UUIDMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@DecoratedWith(PenRequestBatchFileDecorator.class)
@SuppressWarnings("squid:S1214")
public interface PenRequestBatchFileMapper {
  PenRequestBatchFileMapper mapper = Mappers.getMapper(PenRequestBatchFileMapper.class);


  @Mapping(target = "unarchivedFlag", ignore = true)
  @Mapping(target = "unarchivedBatchChangedFlag", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "issuedPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(source = "traxStudentWeb.tswAccount", target = "tswAccount")
  @Mapping(source = "traxStudentWeb.submissionNumber", target = "submissionNumber")
  @Mapping(source = "traxStudentWeb.sourceApplication", target = "sourceApplication")
  @Mapping(source = "traxStudentWeb.ministryPRBSourceCode", target = "ministryPRBSourceCode")
  @Mapping(source = "traxStudentWeb.insertDate", target = "insertDate")
  @Mapping(source = "traxStudentWeb.fileType", target = "fileType")
  @Mapping(source = "traxStudentWeb.fileName", target = "fileName")
  @Mapping(source = "traxStudentWeb.extractDate", target = "extractDate")
  @Mapping(source = "file.batchFileHeader.minCode", target = "minCode")
  @Mapping(source = "file.batchFileHeader.schoolName", target = "schoolName")
  @Mapping(source = "file.batchFileHeader.emailID", target = "email")
  @Mapping(source = "file.batchFileHeader.contactName", target = "contactName")
  @Mapping(source = "file.batchFileHeader.officeNumber", target = "officeNumber")
  @Mapping(source = "file.batchFileTrailer.productID", target = "sisProductID")
  @Mapping(source = "file.batchFileTrailer.productName", target = "sisProductName")
  @Mapping(source = "file.batchFileTrailer.vendorName", target = "sisVendorName")
  @Mapping(source = "file.batchFileTrailer.studentCount", target = "sourceStudentCount")
  PenRequestBatchEntity toPenReqBatchEntity(TraxStudentWeb traxStudentWeb, BatchFile file);

  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "penRequestStudentStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", ignore = true)
  @Mapping(target = "penRequestBatchEntity", ignore = true)
  @Mapping(target = "assignedPEN", ignore = true)
  @Mapping(source = "gender", target = "genderCode")
  @Mapping(source = "birthDate", target = "dob")
  @Mapping(source = "enrolledGradeCode", target = "gradeCode")
  @Mapping(source = "legalGivenName", target = "legalFirstName")
  @Mapping(source = "legalMiddleName", target = "legalMiddleNames")
  @Mapping(source = "legalSurname", target = "legalLastName")
  @Mapping(source = "usualGivenName", target = "usualFirstName")
  @Mapping(source = "usualMiddleName", target = "usualMiddleNames")
  @Mapping(source = "usualSurname", target = "usualLastName")
  @Mapping(source = "localStudentID", target = "localID")
  @Mapping(source = "pen", target = "submittedPen")
  PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(StudentDetails studentDetails);


  @Mapping(target = "unarchivedFlag", ignore = true)
  @Mapping(target = "unarchivedBatchChangedFlag", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "sourceStudentCount", ignore = true)
  @Mapping(target = "sisVendorName", ignore = true)
  @Mapping(target = "sisProductName", ignore = true)
  @Mapping(target = "sisProductID", ignore = true)
  @Mapping(target = "schoolName", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "officeNumber", ignore = true)
  @Mapping(target = "minCode", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "issuedPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "contactName", ignore = true)
  PenRequestBatchEntity toPenReqBatchEntity(TraxStudentWeb traxStudentWeb);
}
