package ca.bc.gov.educ.penreg.api.service;

import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentValidationIssueEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentValidationIssueRepository;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Pen request batch student validation issue service.
 *
 * @author OM
 */
@Service
@Slf4j
public class PenRequestBatchStudentValidationIssueService {
  /**
   * The Repository.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentValidationIssueRepository repository;

  /**
   * Instantiates a new pen request batch student validation issues service.
   *
   * @param repository the repository
   */
  @Autowired
  public PenRequestBatchStudentValidationIssueService(final PenRequestBatchStudentValidationIssueRepository repository) {
    this.repository = repository;
  }

  /**
   * Find pen request batch student validation issues by pen request batch.
   *
   * @param penRequestBatch the pen request batch
   * @return the list of validation issue
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchStudentValidationIssueEntity> findAllPenRequestBatchStudentValidationIssueEntities(final PenRequestBatchEntity penRequestBatch) {
    return this.repository.findByPenRequestBatchStudentEntity_penRequestBatchEntity(penRequestBatch);
  }

  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchStudentValidationIssueEntity> findAllValidationIssuesByPRBStudentID(final UUID penRequestBatchStudentID) {
    val penRequestBatchStudentEntity = new PenRequestBatchStudentEntity();
    penRequestBatchStudentEntity.setPenRequestBatchStudentID(penRequestBatchStudentID);
    return this.repository.findAllByPenRequestBatchStudentEntity(penRequestBatchStudentEntity);
  }

  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public List<PenRequestBatchStudentValidationIssueEntity> findAllValidationIssuesByPRBStudentIDs(final List<UUID> penRequestBatchStudentIDs) {
    return this.repository.findAllByPenRequestBatchStudentEntity_penRequestBatchStudentIDIn(penRequestBatchStudentIDs);
  }
}
