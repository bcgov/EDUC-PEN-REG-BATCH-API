package ca.bc.gov.educ.penreg.api.util;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import lombok.NonNull;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper.PEN_REQUEST_BATCH_API;

/**
 * The PEN Request Batch History util.
 */
public final class PenRequestBatchHistoryUtils {

  /**
   * Create pen req batch history pen request batch history entity.
   *
   * @param entity     the entity
   * @param statusCode the status code
   * @param eventCode  the event code
   * @return the pen request batch history entity
   */
  public static PenRequestBatchHistoryEntity createPenReqBatchHistory(@NonNull final PenRequestBatchEntity entity, final String statusCode, final String eventCode, final String updateUser) {
    PenRequestBatchHistoryEntity penRequestBatchHistoryEntity = new PenRequestBatchHistoryEntity();
    penRequestBatchHistoryEntity.setPenRequestBatchEntity(entity);
    penRequestBatchHistoryEntity.setEventDate(LocalDateTime.now());
    penRequestBatchHistoryEntity.setPenRequestBatchEventCode(eventCode);
    penRequestBatchHistoryEntity.setEventReason(null);
    penRequestBatchHistoryEntity.setSubmissionNumber(entity.getSubmissionNumber());
    penRequestBatchHistoryEntity.setPenRequestBatchStatusCode(entity.getPenRequestBatchStatusCode());
    penRequestBatchHistoryEntity.setPenRequestBatchStatusReason(statusCode);
    penRequestBatchHistoryEntity.setPenRequestBatchTypeCode(entity.getPenRequestBatchTypeCode());
    penRequestBatchHistoryEntity.setMinistryPRBSourceCode(entity.getMinistryPRBSourceCode());
    penRequestBatchHistoryEntity.setSchoolGroupCode(entity.getSchoolGroupCode());
    penRequestBatchHistoryEntity.setFileName(entity.getFileName());
    penRequestBatchHistoryEntity.setFileType(entity.getFileType());
    penRequestBatchHistoryEntity.setInsertDate(entity.getInsertDate());
    penRequestBatchHistoryEntity.setExtractDate(entity.getExtractDate());
    penRequestBatchHistoryEntity.setProcessDate(entity.getProcessDate());
    penRequestBatchHistoryEntity.setSourceApplication(entity.getSourceApplication());
    penRequestBatchHistoryEntity.setMincode(entity.getMincode());
    penRequestBatchHistoryEntity.setSchoolName(entity.getSchoolName());
    penRequestBatchHistoryEntity.setContactName(entity.getContactName());
    penRequestBatchHistoryEntity.setEmail(entity.getEmail());
    penRequestBatchHistoryEntity.setOfficeNumber(entity.getOfficeNumber());
    penRequestBatchHistoryEntity.setSourceStudentCount(entity.getSourceStudentCount());
    penRequestBatchHistoryEntity.setStudentCount(entity.getStudentCount());
    penRequestBatchHistoryEntity.setNewPenCount(entity.getNewPenCount());
    penRequestBatchHistoryEntity.setErrorCount(entity.getErrorCount());
    penRequestBatchHistoryEntity.setMatchedCount(entity.getMatchedCount());
    penRequestBatchHistoryEntity.setRepeatCount(entity.getRepeatCount());
    penRequestBatchHistoryEntity.setFixableCount(entity.getFixableCount());
    penRequestBatchHistoryEntity.setSisVendorName(entity.getSisVendorName());
    penRequestBatchHistoryEntity.setSisProductName(entity.getSisProductName());
    penRequestBatchHistoryEntity.setSisProductID(entity.getSisProductID());
    penRequestBatchHistoryEntity.setCreateUser(PEN_REQUEST_BATCH_API);
    penRequestBatchHistoryEntity.setCreateDate(LocalDateTime.now());
    penRequestBatchHistoryEntity.setUpdateUser(updateUser);
    penRequestBatchHistoryEntity.setUpdateDate(LocalDateTime.now());
    return penRequestBatchHistoryEntity;
  }
}
