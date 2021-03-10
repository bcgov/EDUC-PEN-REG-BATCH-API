package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.constants.MinistryPRBSourceCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchTypeCode.SCHOOL;

/**
 * The type Pen request batch file decorator.
 */
@Slf4j
public abstract class PenRequestBatchFileDecorator implements PenRequestBatchFileMapper {
  /**
   * The Delegate.
   */
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
  protected PenRequestBatchFileDecorator(final PenRequestBatchFileMapper mapper) {
    this.delegate = mapper;
  }

  /**
   * To pen req batch entity loaded pen request batch entity.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @param file             the file
   * @return the pen request batch entity
   */
  @Override
  public PenRequestBatchEntity toPenReqBatchEntityLoaded(final PENWebBlobEntity penWebBlobEntity, final BatchFile file) {
    final var entity = this.delegate.toPenReqBatchEntityLoaded(penWebBlobEntity, file);
    entity.setPenRequestBatchStatusCode(LOADED.getCode());
    entity.setStudentCount((long) file.getStudentDetails().size());
    entity.setSchoolGroupCode(this.computeSchoolGroupCode(file.getBatchFileHeader().getMincode()));
    this.setDefaults(entity);
    return entity;
  }

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
  @Override
  public PenRequestBatchEntity toPenReqBatchEntityForBusinessException(final PENWebBlobEntity penWebBlobEntity, final String reason, final PenRequestBatchStatusCodes penRequestBatchStatusCode, final BatchFile batchFile, final boolean persistStudentRecords) {
    final var entity = this.delegate.toPenReqBatchEntityForBusinessException(penWebBlobEntity, reason, penRequestBatchStatusCode, batchFile, persistStudentRecords);
    entity.setPenRequestBatchStatusCode(penRequestBatchStatusCode.getCode());
    entity.setPenRequestBatchStatusReason(reason);
    this.setDefaults(entity);
    if (batchFile != null) {
      if (batchFile.getStudentDetails() != null) {
        entity.setStudentCount((long) batchFile.getStudentDetails().size());
      }
      if (batchFile.getBatchFileHeader() != null) {
        entity.setSchoolGroupCode(this.computeSchoolGroupCode(batchFile.getBatchFileHeader().getMincode()));
        entity.setMincode(batchFile.getBatchFileHeader().getMincode());
        entity.setSchoolName(batchFile.getBatchFileHeader().getSchoolName());
      }
    }
    if (persistStudentRecords && batchFile != null) { // for certain business exception, system needs to store the student details as well.
      int counter = 1;
      for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
        final var penRequestBatchStudentEntity = this.toPenRequestBatchStudentEntity(student, entity);
        penRequestBatchStudentEntity.setRecordNumber(counter++);
        entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
      }
    }
    return entity;
  }


  /**
   * To pen request batch student entity pen request batch student entity.
   *
   * @param studentDetails        the student details
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen request batch student entity
   */
  @Override
  public PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(final StudentDetails studentDetails, final PenRequestBatchEntity penRequestBatchEntity) {
    final var entity = this.delegate.toPenRequestBatchStudentEntity(studentDetails, penRequestBatchEntity);
    entity.setPenRequestBatchEntity(penRequestBatchEntity); // add thePK/FK relationship
    entity.setPenRequestBatchStudentStatusCode(LOADED.getCode());
    return entity;
  }

  /**
   * Compute school group code string.
   *
   * @param mincode the mincode
   * @return the string
   */
  private String computeSchoolGroupCode(final String mincode) {
    if (mincode == null) {
      return null;
    }
    if (mincode.startsWith(MINCODE_STARTS_WITH_102)) {
      return SchoolGroupCodes.PSI.getCode();
    } else {
      return SchoolGroupCodes.K12.getCode();
    }
  }

  /**
   * Sets defaults.
   *
   * @param penRequestBatchEntity the entity
   */
  private void setDefaults(final PenRequestBatchEntity penRequestBatchEntity) {
    penRequestBatchEntity.setMinistryPRBSourceCode(MinistryPRBSourceCodes.TSW_PEN_WEB.getCode());
    penRequestBatchEntity.setPenRequestBatchTypeCode(SCHOOL.getCode()); // it will be always school for this process.
    penRequestBatchEntity.setExtractDate(LocalDateTime.now());
  }
}
