package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentInfoRequestMacroEntity;
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
    public PenRequestBatchStudentInfoRequestMacroService(PenRequestBatchStudentInfoRequestMacroRepository penRequestMacroRepository) {
        this.penRequestBatchMacroRepository = penRequestMacroRepository;
    }

    public List<PenRequestBatchStudentInfoRequestMacroEntity> findAllMacros() {
        return getPenRequestBatchMacroRepository().findAll();
    }
    public Optional<PenRequestBatchStudentInfoRequestMacroEntity> getMacro(UUID macroId) {
        return getPenRequestBatchMacroRepository().findById(macroId);
    }

    public PenRequestBatchStudentInfoRequestMacroEntity createMacro(PenRequestBatchStudentInfoRequestMacroEntity penRequestBatchEntity) {
        return getPenRequestBatchMacroRepository().save(penRequestBatchEntity);
    }

    public PenRequestBatchStudentInfoRequestMacroEntity updateMacro(UUID macroId, PenRequestBatchStudentInfoRequestMacroEntity entity) {
        val result = getPenRequestBatchMacroRepository().findById(macroId);
        if (result.isPresent()) {
            return getPenRequestBatchMacroRepository().save(entity);
        } else {
            throw new EntityNotFoundException(entity.getClass(), "macroId", macroId.toString());
        }
    }
}
