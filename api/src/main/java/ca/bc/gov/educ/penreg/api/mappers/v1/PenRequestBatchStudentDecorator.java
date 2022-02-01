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

    penRequestBatchStudentEntity.setCreateUser(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getCreateUser()));
    penRequestBatchStudentEntity.setUpdateUser(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getUpdateUser()));

    penRequestBatchStudentEntity.setLegalFirstName(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalFirstName()));
    penRequestBatchStudentEntity.setLegalLastName(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalLastName()));
    penRequestBatchStudentEntity.setLegalMiddleNames(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getLegalMiddleNames()));

    penRequestBatchStudentEntity.setUsualFirstName(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualFirstName()));
    penRequestBatchStudentEntity.setUsualLastName(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualLastName()));
    penRequestBatchStudentEntity.setUsualMiddleNames(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getUsualMiddleNames()));

    penRequestBatchStudentEntity.setLocalID(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getLocalID()));
    penRequestBatchStudentEntity.setPostalCode(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getPostalCode()));

    penRequestBatchStudentEntity.setGenderCode(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getGenderCode()));
    penRequestBatchStudentEntity.setGradeCode(StringMapper.uppercaseAndCleanDiacriticalMarks(penRequestBatchStudent.getGradeCode()));

    return penRequestBatchStudentEntity;
  }
}
