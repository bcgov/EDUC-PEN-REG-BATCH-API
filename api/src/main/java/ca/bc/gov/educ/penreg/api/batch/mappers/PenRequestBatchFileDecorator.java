package ca.bc.gov.educ.penreg.api.batch.mappers;

import ca.bc.gov.educ.penreg.api.batch.input.TraxStudentWeb;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;

/**
 * The type Pen request batch file decorator.
 */
@SuppressWarnings("java:S2140")
public abstract class PenRequestBatchFileDecorator implements PenRequestBatchFileMapper {
  private final PenRequestBatchFileMapper delegate;

  /**
   * Instantiates a new Pen request batch file decorator.
   *
   * @param mapper the mapper
   */
  protected PenRequestBatchFileDecorator(PenRequestBatchFileMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public PenRequestBatchEntity toPenReqBatchEntity(TraxStudentWeb traxStudentWeb, BatchFile file) {
    var entity =  delegate.toPenReqBatchEntity(traxStudentWeb, file);
    entity.setUnarchivedBatchChangedFlag("N");
    entity.setUnarchivedFlag("N");
    return entity;
  }
  @Override
  public PenRequestBatchEntity toPenReqBatchEntity(TraxStudentWeb traxStudentWeb) {
    var entity =  delegate.toPenReqBatchEntity(traxStudentWeb);
    entity.setUnarchivedBatchChangedFlag("N");
    entity.setUnarchivedFlag("N");
    return entity;
  }
}
