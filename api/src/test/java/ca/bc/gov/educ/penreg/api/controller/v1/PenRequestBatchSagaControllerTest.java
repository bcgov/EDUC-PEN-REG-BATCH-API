package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.BasePenRegAPITest;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.v1.SagaMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.v1.Saga;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import ca.bc.gov.educ.penreg.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.v1.ValueType;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchTestUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.*;
import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.AND;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The type Pen request batch saga controller test.
 */
public class PenRequestBatchSagaControllerTest extends BasePenRegAPITest {

  private final String penRequestBatchID = "7f000101-7151-1d84-8171-5187006c0000";
  private final String getPenRequestBatchStudentID = "7f000101-7151-1d84-8171-5187006c0001";
  @Autowired
  PenRequestBatchSagaController controller;
  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;
  @Autowired
  SagaService sagaService;
  @Autowired
  private MockMvc mockMvc;
  private SagaMapper mapper = SagaMapper.mapper;
  @Autowired
  private PenRequestBatchRepository penRequestBatchRepository;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
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
    final var payload = this.placeholderPenRequestBatchStudentActionsSagaData();
    final var sagaFromDB = this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(this.getPenRequestBatchStudentID),
      UUID.fromString(this.penRequestBatchID));
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
      .accept(MediaType.APPLICATION_JSON).content(this.placeholderInvalidPenRequestBatchActionsSagaData())).andDo(print()).andExpect(status().isBadRequest());
  }


  @Test
  public void testIssueNewPen_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/new-pen")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_NEW_PEN_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON).content(this.placeholderPenRequestBatchStudentActionsSagaData())).andDo(print())
      .andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testIssueNewPen_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    final var payload = this.placeholderPenRequestBatchStudentActionsSagaData();
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(this.getPenRequestBatchStudentID),
      UUID.fromString(this.penRequestBatchID));
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
        .content(this.placeholderInvalidPenRequestBatchActionsSagaData()))
      .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(this.placeholderPenRequestBatchStudentActionsSagaData()))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenOtherSagaWithSameStudentIdInProcess_ShouldReturnStatusConflict() throws Exception {
    final var payload = this.placeholderPenRequestBatchStudentActionsSagaData();
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", payload, UUID.fromString(this.getPenRequestBatchStudentID),
      UUID.fromString(this.penRequestBatchID));
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
        .content(this.placeholderPenRequestBatchStudentActionsSagaData()))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }


  @Test
  public void testRepostReports_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(this.placeholderInvalidPenRequestBatchActionsSagaData()))
      .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testRepostReports_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(this.placeholderPenRequestBatchActionsSagaData(this.penRequestBatchID)))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testRepostReports_GivenOtherSagaWithSameBatchInProcess_ShouldReturnStatusConflict() throws Exception {
    final var payload = this.placeholderPenRequestBatchActionsSagaData(this.penRequestBatchID);
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA.toString(), "Test", payload, null,
      UUID.fromString(this.penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testRepostReports_GivenStudentSagaWithSameBatchInProcess_ShouldReturnStatusConflict() throws Exception {
    final var studentPayload = this.placeholderPenRequestBatchStudentActionsSagaData();
    final var payload = this.placeholderPenRequestBatchActionsSagaData(this.penRequestBatchID);
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_USER_MATCH_PROCESSING_SAGA.toString(), "Test", studentPayload, null,
      UUID.fromString(this.penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/repost-reports")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_REPOST_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testProcessStudentRequestMatchedByUser_GivenOtherSagaWithSameBatchIdInProcess_ShouldReturnStatusConflict() throws Exception {
    final var payload = this.placeholderPenRequestBatchStudentActionsSagaData();
    final var parentPayload = this.placeholderPenRequestBatchActionsSagaData(this.penRequestBatchID);
    this.sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_REPOST_REPORTS_SAGA.toString(), "Test", parentPayload, null,
      UUID.fromString(this.penRequestBatchID));
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/user-match")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_USER_MATCH_SAGA")))
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
        .content(this.placeholderMultiplePenRequestBatchActionsSagaData(List.of(UUID.randomUUID(), UUID.randomUUID()))))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  public void testArchiveAndReturnAllFiles_GivenBatchFilesWithMultiples_ShouldReturnStatusBadRequest() throws Exception {
    PenRequestBatchTestUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch.json",
      "mock_pen_req_batch_student.json", 20);
    val ids = this.penRequestBatchRepository.findAll().stream().map(PenRequestBatchEntity::getPenRequestBatchID).collect(Collectors.toList());
    penRequestBatchTestUtils.updateAllStudentRecordsToSameAssignedPen();
    this.mockMvc.perform(post("/api/v1/pen-request-batch-saga/archive-and-return")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_BATCH_ARCHIVE_SAGA")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(this.placeholderMultiplePenRequestBatchActionsSagaData(ids)))
      .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenNoSearchCriteria_shouldReturnAllWithStatusOk() throws Exception {
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_multiple_sagas.json")).getFile()
    );
    final List<Saga> sagaStructs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final List<ca.bc.gov.educ.penreg.api.model.v1.Saga> sagaEntities = sagaStructs.stream().map(mapper::toModel).collect(Collectors.toList());

    for (val saga : sagaEntities) {
      saga.setSagaId(null);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagaEntities);
    final MvcResult result = this.mockMvc
      .perform(get("/api/v1/pen-request-batch-saga/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenNoData_shouldReturnStatusOk() throws Exception {
    final MvcResult result = this.mockMvc
      .perform(get("/api/v1/pen-request-batch-saga/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria_shouldReturnStatusOk() throws Exception {
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_multiple_sagas.json")).getFile()
    );
    final List<Saga> sagaStructs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final List<ca.bc.gov.educ.penreg.api.model.v1.Saga> sagaEntities = sagaStructs.stream().map(mapper::toModel).collect(Collectors.toList());

    for (val saga : sagaEntities) {
      saga.setSagaId(null);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagaEntities);

    final SearchCriteria criteria = SearchCriteria.builder().key("sagaState").operation(FilterOperation.IN).value("IN_PROGRESS").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("sagaName").condition(AND).operation(FilterOperation.EQUAL).value("PEN_REQUEST_BATCH_ARCHIVE_AND_RETURN_SAGA").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final MvcResult result = this.mockMvc
      .perform(get("/api/v1/pen-request-batch-saga/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
        .param("searchCriteriaList", criteriaJSON)
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
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
      "    \"penRequestBatchID\": \"" + this.penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + this.getPenRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\"\n" +
      "  }";
  }

  protected String placeholderPenRequestBatchActionsSagaData(final String penRequestBatchID) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"schoolName\": \"Victoria High School\"\n" +
      "  }";
  }

  protected String placeholderMultiplePenRequestBatchActionsSagaData(final List<UUID> penRequestBatchIDs) {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchArchiveAndReturnSagaData\": [" + penRequestBatchIDs.stream().map(v ->
      this.placeholderPenRequestBatchActionsSagaData(v.toString())).collect(Collectors.joining(",")) + "]\n" +
      "  }";
  }
}
