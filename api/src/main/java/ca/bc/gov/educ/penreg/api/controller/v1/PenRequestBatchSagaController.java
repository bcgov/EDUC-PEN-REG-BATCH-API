package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchSagaEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.penreg.api.mappers.v1.SagaMapper;
import ca.bc.gov.educ.penreg.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.Saga;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static lombok.AccessLevel.PRIVATE;

@RestController
@EnableResourceServer
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
    try {
      var penRequestBatchStudentID = penRequestBatchUserActionsSagaData.getPenRequestBatchStudentID();
      var sagaInProgress = getSagaService().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID,
          PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }

      var saga = getOrchestratorMap()
          .get(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString())
          .startSaga(JsonUtil.getJsonStringFromObject(penRequestBatchUserActionsSagaData),
              penRequestBatchStudentID, penRequestBatchUserActionsSagaData.getPenRequestBatchID());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    } catch (InterruptedException | TimeoutException | IOException e) {
      Thread.currentThread().interrupt();
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> processStudentRequestMatchedByUser(final PenRequestBatchUserActionsSagaData penRequestBatchUserActionsSagaData) {
    var penRequestBatchStudentID = penRequestBatchUserActionsSagaData.getPenRequestBatchStudentID();
    var sagaInProgress = getSagaService().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID,
        PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.name(), getStatusesFilter());
    if (!sagaInProgress.isEmpty()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    try {
      var saga = getOrchestratorMap()
          .get(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString())
          .startSaga(JsonUtil.getJsonStringFromObject(penRequestBatchUserActionsSagaData),
              penRequestBatchStudentID, penRequestBatchUserActionsSagaData.getPenRequestBatchID());
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
