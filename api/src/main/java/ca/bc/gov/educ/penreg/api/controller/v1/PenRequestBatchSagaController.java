package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SagaEnum;
import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchSagaEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.penreg.api.filter.SagaFilterSpecs;
import ca.bc.gov.educ.penreg.api.mappers.v1.ArchiveAndReturnSagaResponseMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.SagaMapper;
import ca.bc.gov.educ.penreg.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.BasePenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUnmatchSagaData;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchUserActionsSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.*;
import static lombok.AccessLevel.PRIVATE;

@RestController
@Slf4j
public class PenRequestBatchSagaController extends PaginatedController implements PenRequestBatchSagaEndpoint {

  @Getter(PRIVATE)
  private final SagaService sagaService;
  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, Orchestrator> orchestratorMap = new HashMap<>();

  private static final SagaMapper sagaMapper = SagaMapper.mapper;

  private static final ArchiveAndReturnSagaResponseMapper archiveAndReturnSagaResponseMapper = ArchiveAndReturnSagaResponseMapper.mapper;

  /**
   * The saga filter specs.
   */
  @Getter(PRIVATE)
  private final SagaFilterSpecs sagaFilterSpecs;

  @Autowired
  public PenRequestBatchSagaController(SagaService sagaService, List<Orchestrator> orchestrators, SagaFilterSpecs sagaFilterSpecs) {
    this.sagaService = sagaService;
    this.sagaFilterSpecs = sagaFilterSpecs;
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

  @Override
  public ResponseEntity<List<ArchiveAndReturnSagaResponse>> archiveAndReturnAllFiles(PenRequestBatchArchiveAndReturnAllSagaData penRequestBatchArchiveAndReturnAllSagaData) {
    return processBatchRequest(PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA, penRequestBatchArchiveAndReturnAllSagaData);
  }

  @Override
  public ResponseEntity<String> repostReports(PenRequestBatchRepostReportsFilesSagaData penRequestBatchRepostReportsSagaData) {
    return processBatchRequest(PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA, penRequestBatchRepostReportsSagaData);
  }

  /**
   * Find all sagas completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @Override
  public CompletableFuture<Page<Saga>> findAllSagas(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    final ObjectMapper objectMapper = new ObjectMapper();
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<ca.bc.gov.educ.penreg.api.model.v1.Saga> sagaEntitySpecification = null;
    try {
      final var associationNames = this.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        final List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        this.getAssociationNamesFromSearchCriterias(associationNames, searches);
        int i = 0;
        for (final var search : searches) {
          sagaEntitySpecification = this.getSpecifications(sagaEntitySpecification, i, search, associationNames, this.getSagaFilterSpecs());
          i++;
        }

      }
    } catch (final JsonProcessingException e) {
      throw new InvalidParameterException(e.getMessage());
    }
    return this.getSagaService().findAll(sagaEntitySpecification, pageNumber, pageSize, sorts).thenApplyAsync(sagas -> sagas.map(sagaMapper::toStruct));
  }


  private ResponseEntity<String> processStudentRequest(SagaEnum sagaName, BasePenRequestBatchStudentSagaData penRequestBatchStudentSagaData) {
    var penRequestBatchStudentID = penRequestBatchStudentSagaData.getPenRequestBatchStudentID();
    var penRequestBatchID = penRequestBatchStudentSagaData.getPenRequestBatchID();
    var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID, getStatusesFilter()).isEmpty();
    var parentSagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(List.of(penRequestBatchID), this.getStatusesFilter()).isEmpty();
    if (sagaInProgress || parentSagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    var orchestrator = getOrchestratorMap().get(sagaName.toString());
    try {
      var saga = orchestrator.createSaga(JsonUtil.getJsonStringFromObject(penRequestBatchStudentSagaData),
        penRequestBatchStudentID, penRequestBatchStudentSagaData.getPenRequestBatchID(), penRequestBatchStudentSagaData.getCreateUser());
      orchestrator.startSaga(saga);
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (JsonProcessingException e) {
      log.error("JsonProcessingException while processStudentRequest", e);
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  private ResponseEntity<String> processBatchRequest(SagaEnum sagaName, PenRequestBatchRepostReportsFilesSagaData penRequestBatchSagaData) {
    var penRequestBatchID = penRequestBatchSagaData.getPenRequestBatchID();

    var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(List.of(penRequestBatchID), this.getStatusesFilter()).isEmpty();

    if (sagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    var orchestrator = getOrchestratorMap().get(sagaName.toString());
    try {
      var saga = orchestrator.createSaga(JsonUtil.getJsonStringFromObject(penRequestBatchSagaData),
        null, penRequestBatchID, penRequestBatchSagaData.getCreateUser());
      orchestrator.startSaga(saga);
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (JsonProcessingException e) {
      log.error("JsonProcessingException while processStudentRequest", e);
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  private ResponseEntity<List<ArchiveAndReturnSagaResponse>> processBatchRequest(SagaEnum sagaName, PenRequestBatchArchiveAndReturnAllSagaData penRequestBatchArchiveAndReturnAllSagaData) {
    var penRequestBatchIDs = penRequestBatchArchiveAndReturnAllSagaData.getPenRequestBatchArchiveAndReturnSagaData()
            .stream().map(PenRequestBatchArchiveAndReturnSagaData::getPenRequestBatchID).collect(Collectors.toList());

    var sagaInProgress = !this.getSagaService().findAllByPenRequestBatchIDInAndStatusIn(penRequestBatchIDs, this.getStatusesFilter()).isEmpty();

    if (sagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    try {
      var updateUser = penRequestBatchArchiveAndReturnAllSagaData.getUpdateUser();
      var payloads = penRequestBatchArchiveAndReturnAllSagaData.getPenRequestBatchArchiveAndReturnSagaData().stream().map(sagaData -> {
        sagaData.setUpdateUser(updateUser);
        try {
          val payload = JsonUtil.getJsonStringFromObject(sagaData);
          return Pair.of(sagaData.getPenRequestBatchID(), payload);
        } catch (JsonProcessingException e) {
          throw new InvalidParameterException(e.getMessage());
        }
      }).collect(Collectors.toList());

      var sagas = this.getOrchestratorMap()
        .get(sagaName.toString())
        .saveMultipleSagas(payloads, penRequestBatchArchiveAndReturnAllSagaData.getCreateUser());
      for (val saga : sagas) {
        this.getOrchestratorMap()
          .get(sagaName.toString())
          .startSaga(saga);
      }
      return ResponseEntity.ok(sagas.stream().map(archiveAndReturnSagaResponseMapper::toStruct).collect(Collectors.toList()));
    } catch (final Exception e) {
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
