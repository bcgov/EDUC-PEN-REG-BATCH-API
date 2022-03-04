package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.batch.mappers.StringMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PenRequestBatchStudentDecorator implements PenRequestBatchStudentMapper {

  /**
   * The Delegate
   */
  private final PenRequestBatchStudentMapper delegate;

  protected PenRequestBatchStudentDecorator(final PenRequestBatchStudentMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public PenRequestBatchStudentEntity toModel(PenRequestBatchStudent penRequestBatchStudent) {
    var penRequestBatchStudentEntity = delegate.toModel(penRequestBatchStudent);

    penRequestBatchStudentEntity.setCreateUser(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getCreateUser()));
    penRequestBatchStudentEntity.setUpdateUser(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getUpdateUser()));

    penRequestBatchStudentEntity.setLegalFirstName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getLegalFirstName()));
    penRequestBatchStudentEntity.setLegalLastName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getLegalLastName()));
    penRequestBatchStudentEntity.setLegalMiddleNames(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getLegalMiddleNames()));

    penRequestBatchStudentEntity.setUsualFirstName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getUsualFirstName()));
    penRequestBatchStudentEntity.setUsualLastName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getUsualLastName()));
    penRequestBatchStudentEntity.setUsualMiddleNames(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getUsualMiddleNames()));

    penRequestBatchStudentEntity.setLocalID(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getLocalID()));
    penRequestBatchStudentEntity.setPostalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getPostalCode()));

    penRequestBatchStudentEntity.setGenderCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getGenderCode()));
    penRequestBatchStudentEntity.setGradeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(penRequestBatchStudent.getGradeCode()));

    return penRequestBatchStudentEntity;
  }
}
