package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.constants.MinistryPRBSourceCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.constants.UnarchivedBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes.STATUS_CHANGED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOADED;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes.LOAD_FAIL;
import static ca.bc.gov.educ.penreg.api.constants.PenRequestBatchTypeCode.SCHOOL;

/**
 * The type Pen request batch file decorator.
 */
@SuppressWarnings("java:S2140")
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
  protected PenRequestBatchFileDecorator(PenRequestBatchFileMapper mapper) {
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
  public PenRequestBatchEntity toPenReqBatchEntityLoaded(PENWebBlobEntity penWebBlobEntity, BatchFile file) {
    var entity = delegate.toPenReqBatchEntityLoaded(penWebBlobEntity, file);
    setDefaults(entity);
    entity.setPenRequestBatchStatusCode(LOADED.getCode());
    entity.setStudentCount((long) file.getStudentDetails().size());
    entity.setSchoolGroupCode(computeSchoolGroupCode(file.getBatchFileHeader().getMincode()));
    PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(entity, LOADED.getCode(),STATUS_CHANGED.getCode(), null);
    entity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
    return entity;
  }

  /**
   * Create pen req batch history pen request batch history entity.
   *
   * @param entity     the entity
   * @param statusCode the status code
   * @param eventCode  the event code
   * @param reason     the reason
   * @return the pen request batch history entity
   */
  private PenRequestBatchHistoryEntity createPenReqBatchHistory(@NonNull PenRequestBatchEntity entity, String statusCode, String eventCode, String reason) {
    var penRequestBatchHistory = new PenRequestBatchHistoryEntity();
    penRequestBatchHistory.setCreateDate(LocalDateTime.now());
    penRequestBatchHistory.setUpdateDate(LocalDateTime.now());
    penRequestBatchHistory.setPenRequestBatchEntity(entity);
    penRequestBatchHistory.setPenRequestBatchStatusCode(statusCode);
    penRequestBatchHistory.setPenRequestBatchEventCode(eventCode);
    penRequestBatchHistory.setCreateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setUpdateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistory.setEventDate(LocalDateTime.now());
    penRequestBatchHistory.setEventReason(reason);
    return penRequestBatchHistory;
  }


  /**
   * To pen req batch entity load fail pen request batch entity.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @param reason           the reason
   * @return the pen request batch entity
   */
  @Override
  public PenRequestBatchEntity toPenReqBatchEntityLoadFail(PENWebBlobEntity penWebBlobEntity, String reason) {
    var entity = delegate.toPenReqBatchEntityLoadFail(penWebBlobEntity, reason);
    setDefaults(entity);
    entity.setPenRequestBatchStatusCode(LOAD_FAIL.getCode());
    entity.setPenRequestBatchStatusReason(reason);
    PenRequestBatchHistoryEntity penRequestBatchHistory = createPenReqBatchHistory(entity, LOAD_FAIL.getCode(),STATUS_CHANGED.getCode(), reason);
    entity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
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
  public PenRequestBatchStudentEntity toPenRequestBatchStudentEntity(StudentDetails studentDetails, PenRequestBatchEntity penRequestBatchEntity){
    var entity = delegate.toPenRequestBatchStudentEntity(studentDetails,penRequestBatchEntity);
    entity.setPenRequestBatchEntity(penRequestBatchEntity); // add thePK/FK relationship
    entity.setPenRequestBatchStudentStatusCode(LOADED.getCode());
    return entity;
  }

  /**
   * Compute school group code string.
   *
   * @param mincode the min code
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
   * @param entity the entity
   */
  private void setDefaults(PenRequestBatchEntity entity) {
    entity.setUnarchivedBatchChangedFlag("N");
    entity.setUnarchivedBatchStatusCode(UnarchivedBatchStatusCodes.NA.getCode());
    entity.setMinistryPRBSourceCode(MinistryPRBSourceCodes.TSW_PEN_WEB.getCode());
    entity.setPenRequestBatchTypeCode(SCHOOL.getCode()); // it will be always school for this process.
    entity.setExtractDate(LocalDateTime.now());
  }
}
