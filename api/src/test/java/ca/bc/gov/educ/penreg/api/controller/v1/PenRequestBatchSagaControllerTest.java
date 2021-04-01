package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PenRequestBatchSagaControllerTest {

  @Autowired
  PenRequestBatchSagaController controller;

  @Autowired
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
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testIssueNewPen_GivenInValidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch-saga/" + UUID.randomUUID().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_READ_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isNotFound());
  }
  @Test
  public void testIssueNewPen_GivenValidID_ShouldReturnStatusOK() throws Exception {
    var payload = placeholderPenRequestBatchStudentActionsSagaData();
    var sagaFromDB = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(get("/api/v1/pen-request-batch-saga/" + sagaFromDB.getSagaId().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_READ_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk());
  }
  @Test
  public void testIssueNewPen_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_NEW_PEN_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(placeholderInvalidPenRequestBatchActionsSagaData())).andDo(print()).andExpect(status().isBadRequest());
  }


  @Test
  public void testIssueNewPen_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_NEW_PEN_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(placeholderPenRequestBatchStudentActionsSagaData())).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testIssueNewPen_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    var payload = placeholderPenRequestBatchStudentActionsSagaData();
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_NEW_PEN_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(placeholderInvalidPenRequestBatchActionsSagaData()))
                .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(placeholderPenRequestBatchStudentActionsSagaData()))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    var payload = placeholderPenRequestBatchStudentActionsSagaData();
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(getPenRequestBatchStudentID),
        UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testProcessStudentRequestUnmatchedByUser_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-unmatch")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(placeholderPenRequestBatchStudentActionsSagaData()))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }


  @Test
  public void testRepostReports_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(placeholderInvalidPenRequestBatchActionsSagaData()))
      .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testRepostReports_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(placeholderPenRequestBatchActionsSagaData(penRequestBatchID)))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testRepostReports_GivenOtherSagaWithSameBatchInProcess_ShouldReturnStatusConflict() throws Exception {
    var payload = placeholderPenRequestBatchActionsSagaData(penRequestBatchID);
    sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA.toString(), "Test", payload, null,
      UUID.fromString(penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testArchiveAndReturnAllFiles_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/archive-and-return")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_ARCHIVE_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(placeholderMultiplePenRequestBatchActionsSagaData(List.of(UUID.randomUUID(), UUID.randomUUID()))))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
  }


  protected String placeholderInvalidPenRequestBatchActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "  }";
  }

  protected String placeholderPenRequestBatchStudentActionsSagaData() {
    return " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
        "    \"penRequestBatchStudentID\": \"" + getPenRequestBatchStudentID + "\",\n" +
        "    \"legalFirstName\": \"Jack\"\n" +
        "  }";
  }

  protected String placeholderPenRequestBatchActionsSagaData(String penRequestBatchID) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"schoolName\": \"Victoria High School\"\n" +
      "  }";
  }

  protected String placeholderMultiplePenRequestBatchActionsSagaData(List<UUID> penRequestBatchIDs) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchArchiveAndReturnSagaData\": [" + penRequestBatchIDs.stream().map(v ->
              placeholderPenRequestBatchActionsSagaData(v.toString())).collect(Collectors.joining(",")) + "]\n" +
      "  }";
  }
}
