package ca.bc.gov.educ.penreg.api.helpers;

import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class to provide some common methods that can be called from components, as a single source of truth for
 * easier maintainable code.
 */
@Slf4j
public final class PenRegBatchHelper {
  private PenRegBatchHelper() {

  }

  public static PenRequestBatchStudentSagaData createSagaDataFromStudentRequestAndBatch(@NonNull final PenRequestBatchStudentEntity penRequestBatchStudentEntity, @NonNull final PenRequestBatchEntity penRequestBatchEntity) {
    val sagaData = PenRegBatchHelper.convertPRBStudentToSagaData(penRequestBatchStudentEntity);
    return PenRegBatchHelper.updateEachSagaRecord(sagaData, penRequestBatchEntity);
  }

  public static Set<PenRequestBatchStudentSagaData> createSagaDataFromStudentRequestsAndBatch(@NonNull final Set<PenRequestBatchStudentEntity> penRequestBatchStudentEntities, @NonNull final PenRequestBatchEntity penRequestBatchEntity) {
    return penRequestBatchStudentEntities.stream()
        .map(PenRegBatchHelper::convertPRBStudentToSagaData)
        .map(element -> updateEachSagaRecord(element, penRequestBatchEntity))
        .collect(Collectors.toSet());
  }

  private static PenRequestBatchStudentSagaData convertPRBStudentToSagaData(@NonNull final PenRequestBatchStudentEntity penRequestBatchStudentEntity) {
    return PenRequestBatchStudentSagaDataMapper.mapper.toPenReqBatchStudentSagaData(penRequestBatchStudentEntity);
  }

  private static PenRequestBatchStudentSagaData updateEachSagaRecord(final PenRequestBatchStudentSagaData element,
                                                                     final PenRequestBatchEntity penRequestBatchEntity) {
    element.setMincode(penRequestBatchEntity.getMincode());
    element.setPenRequestBatchID(penRequestBatchEntity.getPenRequestBatchID());
    return element;
  }
}
