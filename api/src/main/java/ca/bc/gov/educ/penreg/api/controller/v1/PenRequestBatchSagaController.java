package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SagaEnum;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchSagaEndpoint;
import ca.bc.gov.educ.penreg.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.penreg.api.mappers.v1.SagaMapper;
import ca.bc.gov.educ.penreg.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.Saga;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA;
import static lombok.AccessLevel.PRIVATE;

@RestController
@Slf4j
public class PenRequestBatchSagaController implements PenRequestBatchSagaEndpoint {

  @Getter(PRIVATE)
  private final SagaService sagaService;
  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, Orchestrator> orchestratorMap = new HashMap<>();

  private static final SagaMapper sagaMapper = SagaMapper.mapper;

  @Autowired
  public PenRequestBatchSagaController(SagaService sagaService, List<Orchestrator> orchestrators) {
    this.sagaService = sagaService;
    orchestrators.forEach(orchestrator -> orchestratorMap.put(orchestrator.getSagaName(), orchestrator));
    log.info("'{}' Saga Orchestrators are loaded.", String.join(",", orchestratorMap.keySet()));
  }

  @Override
  public ResponseEntity<Saga> readSaga(UUID sagaID) {
    return getSagaService().findSagaById(sagaID)
                           .map(sagaMapper::toStruct)
                           .map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @Override
  public ResponseEntity<String> issueNewPen(PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return processStudentRequest(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA, penRequestBatchUserActionsSagaData);
  }

  @Override
  public ResponseEntity<String> processStudentRequestMatchedByUser(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    return processStudentRequest(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA, penRequestBatchUserActionsSagaData);
  }

  @Override
  public ResponseEntity<String> processStudentRequestUnmatchedByUser(final PenRequestBatchUnmatchSagaData penRequestBatchUnmatchSagaData) {
    return processStudentRequest(PEN_REQUEST_BATCH_USER_UNMATCH_PROCESSING_SAGA, penRequestBatchUnmatchSagaData);
  }

  private ResponseEntity<String> processStudentRequest(SagaEnum sagaName, BasePenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    var penRequestBatchStudentID = penRequestBatchStudentSagaData.getPenRequestBatchStudentID();
    var sagaInProgress = getSagaService().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID,
      sagaName.toString(), getStatusesFilter());
    if (!sagaInProgress.isEmpty()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    try {
      var saga = getOrchestratorMap()
        .get(sagaName.toString())
        .startSaga(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData),
          penRequestBatchStudentID, penRequestBatchStudentSagaData.getPenRequestBatchID(), penRequestBatchStudentSagaData.getCreateUser());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (InterruptedException | TimeoutException | IOException e) {
      Thread.currentThread().interrupt();
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  protected List<String> getStatusesFilter() {
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    return statuses;
  }

}
