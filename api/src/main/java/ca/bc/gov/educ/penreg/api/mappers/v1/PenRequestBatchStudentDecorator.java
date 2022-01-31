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

    penRequestBatchStudentEntity.setCreateUser(StringMapper.toUpperCase(penRequestBatchStudent.getCreateUser()));
    penRequestBatchStudentEntity.setUpdateUser(StringMapper.toUpperCase(penRequestBatchStudent.getUpdateUser()));

    penRequestBatchStudentEntity.setLegalFirstName(StringMapper.toUpperCase(penRequestBatchStudent.getLegalFirstName()));
    penRequestBatchStudentEntity.setLegalLastName(StringMapper.toUpperCase(penRequestBatchStudent.getLegalLastName()));
    penRequestBatchStudentEntity.setLegalMiddleNames(StringMapper.toUpperCase(penRequestBatchStudent.getLegalMiddleNames()));

    penRequestBatchStudentEntity.setUsualFirstName(StringMapper.toUpperCase(penRequestBatchStudent.getUsualFirstName()));
    penRequestBatchStudentEntity.setUsualLastName(StringMapper.toUpperCase(penRequestBatchStudent.getUsualLastName()));
    penRequestBatchStudentEntity.setUsualMiddleNames(StringMapper.toUpperCase(penRequestBatchStudent.getUsualMiddleNames()));

    penRequestBatchStudentEntity.setLocalID(StringMapper.toUpperCase(penRequestBatchStudent.getLocalID()));
    penRequestBatchStudentEntity.setPostalCode(StringMapper.toUpperCase(penRequestBatchStudent.getPostalCode()));

    penRequestBatchStudentEntity.setGenderCode(StringMapper.toUpperCase(penRequestBatchStudent.getGenderCode()));
    penRequestBatchStudentEntity.setGradeCode(StringMapper.toUpperCase(penRequestBatchStudent.getGradeCode()));

    return penRequestBatchStudentEntity;
  }
}
