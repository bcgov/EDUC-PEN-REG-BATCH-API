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

    penRequestBatchStudentEntity.setCreateUser(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getCreateUser()));
    penRequestBatchStudentEntity.setUpdateUser(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getUpdateUser()));

    penRequestBatchStudentEntity.setLegalFirstName(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalFirstName()));
    penRequestBatchStudentEntity.setLegalLastName(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalLastName()));
    penRequestBatchStudentEntity.setLegalMiddleNames(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalMiddleNames()));

    penRequestBatchStudentEntity.setUsualFirstName(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualFirstName()));
    penRequestBatchStudentEntity.setUsualLastName(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualLastName()));
    penRequestBatchStudentEntity.setUsualMiddleNames(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualMiddleNames()));

    penRequestBatchStudentEntity.setLocalID(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getLocalID()));
    penRequestBatchStudentEntity.setPostalCode(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getPostalCode()));

    penRequestBatchStudentEntity.setGenderCode(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getGenderCode()));
    penRequestBatchStudentEntity.setGradeCode(StringMapper.uppercaseTrimAndCleanDiacriticalMarks(penRequestBatchStudent.getGradeCode()));

    return penRequestBatchStudentEntity;
  }
}
