package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.exception.RestExceptionHandler;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.support.WithMockOAuth2Scope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The type Pen request batch saga controller test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PenRequestBatchSagaControllerTest {

  @Autowired
  PenRequestBatchSagaController controller;

  private MockMvc mockMvc;

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  SagaService sagaService;

  private final String penRequestBatchID = "7f000101-7151-1d84-8171-5187006c0000";

  private final String getPenRequestBatchStudentID = "7f000101-7151-1d84-8171-5187006c0001";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
                             .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_READ_SAGA")
  public void testIssueNewPen_GivenInValidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch-saga/" + UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isNotFound());
  }
  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_READ_SAGA")
  public void testIssueNewPen_GivenValidID_ShouldReturnStatusOK() throws Exception {
    var payload = placeholderPenRequestBatchActionsSagaData();
    var sagaFromDB = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(get("/api/v1/pen-request-batch-saga/" + sagaFromDB.getSagaId().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk());
  }
  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_NEW_PEN_SAGA")
  public void testIssueNewPen_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen").contentType(MediaType.APPLICATION_JSON)
                                                                       .accept(MediaType.APPLICATION_JSON).content(placeholderInvalidPenRequestBatchActionsSagaData())).andDo(print()).andExpect(status().isBadRequest());
  }


  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_NEW_PEN_SAGA")
  public void testIssueNewPen_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen").contentType(MediaType.APPLICATION_JSON)
                                                                       .accept(MediaType.APPLICATION_JSON).content(placeholderPenRequestBatchActionsSagaData())).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_NEW_PEN_SAGA")
  public void testIssueNewPen_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    var payload = placeholderPenRequestBatchActionsSagaData();
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen").contentType(MediaType.APPLICATION_JSON)
                                                                       .accept(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_USER_MATCH_SAGA")
  public void testProcessStudentRequestMatchedByUser_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(placeholderInvalidPenRequestBatchActionsSagaData()))
                .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_USER_MATCH_SAGA")
  public void testProcessStudentRequestMatchedByUser_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(placeholderPenRequestBatchActionsSagaData()))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_BATCH_USER_MATCH_SAGA")
  public void testProcessStudentRequestMatchedByUser_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    var payload = placeholderPenRequestBatchActionsSagaData();
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(payload)).andDo(print()).andExpect(status().isConflict());
  }


  protected String placeholderInvalidPenRequestBatchActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "  }";
  }

  protected String placeholderPenRequestBatchActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + getPenRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\"\n" +
        "  }";
  }
}
