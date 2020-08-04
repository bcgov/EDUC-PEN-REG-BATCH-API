package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.constants.MinistryPRBSourceCodes;
import ca.bc.gov.educ.penreg.api.batch.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.batch.constants.UnarchivedBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.batch.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.batch.constants.PenRequestBatchStatusCodes.LOAD_FAIL;
import static ca.bc.gov.educ.penreg.api.batch.constants.PenRequestBatchTypeCode.SCHOOL;

/**
 * The type Pen request batch file decorator.
 */
@SuppressWarnings("java:S2140")
@Slf4j
public abstract class PenRequestBatchFileDecorator implements PenRequestBatchFileMapper {
  private final PenRequestBatchFileMapper delegate;
  /**
   * The constant MINCODE_STARTS_WITH_102.
   */
  public static final String MINCODE_STARTS_WITH_102 = "102";

  /**
   * Instantiates a new Pen request batch file decorator.
   *
   * @param mapper the mapper
   */
  protected PenRequestBatchFileDecorator(PenRequestBatchFileMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public PenRequestBatchEntity toPenReqBatchEntityLoaded(PENWebBlobEntity penWebBlobEntity, BatchFile file) {
    var entity = delegate.toPenReqBatchEntityLoaded(penWebBlobEntity, file);
    setDefaults(entity);
    entity.setPenRequestBatchStatusCode(LOADED.getCode());
    entity.setStudentCount((long) file.getStudentDetails().size());
    entity.setSchoolGroupCode(computeSchoolGroupCode(file.getBatchFileHeader().getMinCode()));
    return entity;
  }



  @Override
  public PenRequestBatchEntity toPenReqBatchEntityLoadFail(PENWebBlobEntity penWebBlobEntity, String reason) {
    var entity = delegate.toPenReqBatchEntityLoadFail(penWebBlobEntity, reason);
    setDefaults(entity);
    entity.setPenRequestBatchStatusCode(LOAD_FAIL.getCode());
    entity.setPenRequestBatchStatusReason(reason);
    return entity;
  }

  @Override
  public PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(StudentDetails studentDetails, PenRequestBatchEntity penRequestBatchEntity){
    var entity = delegate.toPenRequestBatchStudentEntity(studentDetails,penRequestBatchEntity);
    entity.setPenRequestBatchEntity(penRequestBatchEntity); // add thePK/FK relationship
    entity.setPenRequestBatchStudentStatusCode(LOADED.getCode());
    return entity;
  }

  private String computeSchoolGroupCode(final String minCode) {
    if (minCode == null) {
      return null;
    }
    if (minCode.startsWith(MINCODE_STARTS_WITH_102)) {
      return SchoolGroupCodes.PSI.getCode();
    } else {
      return SchoolGroupCodes.K12.getCode();
    }
  }
  private void setDefaults(PenRequestBatchEntity entity) {
    entity.setUnarchivedBatchChangedFlag("N");
    entity.setUnarchivedBatchStatusCode(UnarchivedBatchStatusCodes.NA.getCode());
    entity.setMinistryPRBSourceCode(MinistryPRBSourceCodes.TSW_PEN_WEB.getCode());
    entity.setPenRequestBatchTypeCode(SCHOOL.getCode()); // it will be always school for this process.
    entity.setExtractDate(LocalDateTime.now());
  }
}
