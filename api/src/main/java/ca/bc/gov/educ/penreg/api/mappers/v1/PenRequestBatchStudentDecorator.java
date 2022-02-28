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

    penRequestBatchStudentEntity.setCreateUser(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getCreateUser()));
    penRequestBatchStudentEntity.setUpdateUser(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getUpdateUser()));

    penRequestBatchStudentEntity.setLegalFirstName(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getLegalFirstName()));
    penRequestBatchStudentEntity.setLegalLastName(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getLegalLastName()));
    penRequestBatchStudentEntity.setLegalMiddleNames(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getLegalMiddleNames()));

    penRequestBatchStudentEntity.setUsualFirstName(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getUsualFirstName()));
    penRequestBatchStudentEntity.setUsualLastName(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getUsualLastName()));
    penRequestBatchStudentEntity.setUsualMiddleNames(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getUsualMiddleNames()));

    penRequestBatchStudentEntity.setLocalID(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getLocalID()));
    penRequestBatchStudentEntity.setPostalCode(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getPostalCode()));

    penRequestBatchStudentEntity.setGenderCode(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getGenderCode()));
    penRequestBatchStudentEntity.setGradeCode(StringMapper.uppercaseAndTrim(penRequestBatchStudent.getGradeCode()));

    return penRequestBatchStudentEntity;
  }
}
