package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.penreg.api.endpoint.v1.PenRequestBatchSagaEndpoint;
import ca.bc.gov.educ.penreg.api.exception.InvalidParameterException;
import ca.bc.gov.educ.penreg.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchNewPenOrchestrator;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchNewPenSagaData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static lombok.AccessLevel.PRIVATE;

@RestController
@EnableResourceServer
@Slf4j
public class PenRequestBatchSagaController implements PenRequestBatchSagaEndpoint {

    @Getter(PRIVATE)
    private final SagaService sagaService;
    @Getter(PRIVATE)
    private final PenReqBatchNewPenOrchestrator penReqBatchNewPenOrchestrator;

    @Autowired
    public PenRequestBatchSagaController(SagaService sagaService, PenReqBatchNewPenOrchestrator penReqBatchNewPenOrchestrator) {
        this.sagaService = sagaService;
        this.penReqBatchNewPenOrchestrator = penReqBatchNewPenOrchestrator;
    }

    @Override
    public ResponseEntity<String> issueNewPen(PenRequestBatchNewPenSagaData penRequestBatchNewPenSagaData) {
        try {
            var penRequestBatchStudentID = penRequestBatchNewPenSagaData.getPenRequestBatchStudentID();
            var sagaInProgress = getSagaService().findAllByPenRequestBatchStudentIDAndStatusIn(penRequestBatchStudentID,
              PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), getStatusesFilter());
            if (!sagaInProgress.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            var saga = getPenReqBatchNewPenOrchestrator().startSaga(JsonUtil.getJsonStringFromObject(penRequestBatchNewPenSagaData),
              penRequestBatchStudentID, penRequestBatchNewPenSagaData.getPenRequestBatchID());
            return ResponseEntity.ok(saga.getSagaId().toString());
        } catch (JsonProcessingException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (InterruptedException | TimeoutException | IOException e) {
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
