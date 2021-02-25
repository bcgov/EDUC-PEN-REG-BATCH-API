package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentInfoRequestMacroEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentInfoRequestMacroRepository;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Service
public class PenRequestBatchStudentInfoRequestMacroService {

  @Getter(PRIVATE)
  private final PenRequestBatchStudentInfoRequestMacroRepository penRequestBatchMacroRepository;

  @Autowired
  public PenRequestBatchStudentInfoRequestMacroService(final PenRequestBatchStudentInfoRequestMacroRepository penRequestMacroRepository) {
    this.penRequestBatchMacroRepository = penRequestMacroRepository;
  }

  public List<PenRequestBatchStudentInfoRequestMacroEntity> findAllMacros() {
    return this.getPenRequestBatchMacroRepository().findAll();
  }

  public Optional<PenRequestBatchStudentInfoRequestMacroEntity> getMacro(final UUID macroId) {
    return this.getPenRequestBatchMacroRepository().findById(macroId);
  }

  public PenRequestBatchStudentInfoRequestMacroEntity createMacro(final PenRequestBatchStudentInfoRequestMacroEntity penRequestBatchEntity) {
    return this.getPenRequestBatchMacroRepository().save(penRequestBatchEntity);
  }

  public PenRequestBatchStudentInfoRequestMacroEntity updateMacro(final UUID macroId, final PenRequestBatchStudentInfoRequestMacroEntity entity) {
    val result = this.getPenRequestBatchMacroRepository().findById(macroId);
    if (result.isPresent()) {
      return this.getPenRequestBatchMacroRepository().save(entity);
    } else {
      throw new EntityNotFoundException(entity.getClass(), "macroId", macroId.toString());
    }
  }
}
