package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.exception.RestExceptionHandler;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.*;
import ca.bc.gov.educ.penreg.api.support.WithMockOAuth2Scope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.OR;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The type Pen request batch api controller test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PenRequestBatchAPIControllerTest {
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
  /**
   * The Mock mvc.
   */
  private MockMvc mockMvc;
  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  /**
   * The Pen request batch api controller.
   */
  @Autowired
  PenRequestBatchAPIController penRequestBatchAPIController;
  /**
   * The Pen request batch repository.
   */
  @Autowired
  PenRequestBatchRepository penRequestBatchRepository;

  /**
   * Sets up.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(penRequestBatchAPIController)
        .setControllerAdvice(new RestExceptionHandler()).build();
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    penRequestBatchRepository.deleteAll();
  }

  /**
   * Test read pen request batch paginated always should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_Always_ShouldReturnStatusOk() throws Exception {
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  /**
   * Test read pen request batch paginated given fixable count filter without data should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenFixableCountFilterWithoutData_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("fixableCount").operation(FilterOperation.GREATER_THAN).value("10").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  /**
   * Test read pen request batch paginated given school group code filter should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  /**
   * Test read pen request batch paginated given school group code or pen req batch status code filter and matched count or fixable count should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeOrPenReqBatchStatusCodeFilterAndMatchedCountOrFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Test read pen request batch paginated given school group code and pen req batch status code filter and matched count or fixable count should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeAndPenReqBatchStatusCodeFilterAndMatchedCountOrFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(AND).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  /**
   * Test read pen request batch paginated given school group code and pen req batch status code filter or matched count and fixable count should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeANDPenReqBatchStatusCodeFilterORMatchedCountANDFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(AND).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(AND).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(OR).searchCriteriaList(criteriaList1).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Test read pen request batch paginated given school group code or pen req batch status code filter or sis product name and matched count or fixable count or source student count should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeOrPenReqBatchStatusCodeFilterOrSisProductNameANDMatchedCountORFixableCountORSourceStudentCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    SearchCriteria criteria3 = SearchCriteria.builder().key("sisProductName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("MYED").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);
    SearchCriteria criteria4 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria5 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria6 = SearchCriteria.builder().key("sourceStudentCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria4);
    criteriaList1.add(criteria5);
    criteriaList1.add(criteria6);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Test read pen request batch paginated given multiple group condition should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenMultipleGroupCondition_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    penRequestBatchRepository.saveAll(models);
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    SearchCriteria criteria3 = SearchCriteria.builder().key("sisProductName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("MYED").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);
    SearchCriteria criteria4 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria5 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria6 = SearchCriteria.builder().key("sourceStudentCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria4);
    criteriaList1.add(criteria5);
    criteriaList1.add(criteria6);
    SearchCriteria criteria7 = SearchCriteria.builder().key("studentCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    SearchCriteria criteria8 = SearchCriteria.builder().key("sisVendorName").condition(OR).operation(FilterOperation.EQUAL).value("Follett Software").valueType(ValueType.STRING).build();
    SearchCriteria criteria9 = SearchCriteria.builder().key("minCode").condition(OR).operation(FilterOperation.EQUAL).value("09898027").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList2 = new LinkedList<>();
    criteriaList1.add(criteria7);
    criteriaList1.add(criteria8);
    criteriaList1.add(criteria9);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList2).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Test read pen request batch given invalid id should return status not found.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatch_GivenInvalidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + UUID.randomUUID())).andDo(print()).andExpect(status().isNotFound());
  }

  /**
   * Test read pen request batch given valid id should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatch_GivenValidID_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(this::populateAuditColumns).collect(Collectors.toList());
    var results = penRequestBatchRepository.saveAll(models);
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + results.iterator().next().getPenRequestBatchID())).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Populate audit columns pen request batch entity.
   *
   * @param model the model
   * @return the pen request batch entity
   */
  private PenRequestBatchEntity populateAuditColumns(PenRequestBatchEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
    return model;
  }
}
