package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import ca.bc.gov.educ.penreg.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.v1.ValueType;
import ca.bc.gov.educ.penreg.api.support.PenRequestBatchUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.OR;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PenRequestBatchHistoryAPIControllerTest {
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
  /**
   * The Mock mvc.
   */
  @Autowired
  private MockMvc mockMvc;
  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  /**
   * The Pen request batch history api controller.
   */
  @Autowired
  PenRequestBatchHistoryAPIController penRequestBatchHistoryAPIController;
  /**
   * The Pen request batch repository.
   */
  @Autowired
  PenRequestBatchRepository penRequestBatchRepository;
  /**
   * The Pen request batch history repository.
   */
  @Autowired
  PenRequestBatchHistoryRepository penRequestBatchHistoryRepository;

  /**
   * Sets up.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    this.penRequestBatchHistoryRepository.deleteAll();
  }

  /**
   * Test read pen request batch paginated always should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_Always_ShouldReturnStatusOk() throws Exception {
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
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
  public void testReadPenRequestBatchPaginated_GivenFixableCountFilterWithoutData_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("fixableCount").operation(FilterOperation.GREATER_THAN).value("10").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
            .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
            .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  /**
   * Test read pen request batch paginated given school group code filter should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_GivenStatusFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestBatchStatusCode").operation(FilterOperation.EQUAL).value(PenRequestBatchStatusCodes.LOADED.getCode()).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
            .perform(get("/api/v1/pen-request-batch-history/paginated")
                    .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
                    .param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  /**
   * Test read pen request batch paginated given school group code or pen req batch status code filter and matched count or fixable count should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeOrPenReqBatchStatusCodeFilterAndMatchedCountOrFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
            .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeAndPenReqBatchStatusCodeFilterAndMatchedCountOrFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(AND).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
            .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeANDPenReqBatchStatusCodeFilterORMatchedCountANDFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(AND).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final SearchCriteria criteria3 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria4 = SearchCriteria.builder().key("fixableCount").condition(AND).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(OR).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
                .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeOrPenReqBatchStatusCodeFilterOrSisProductNameANDMatchedCountORFixableCountORSourceStudentCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    final SearchCriteria criteria3 = SearchCriteria.builder().key("sisProductName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("MYED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);
    final SearchCriteria criteria4 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria5 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria6 = SearchCriteria.builder().key("sourceStudentCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria4);
    criteriaList1.add(criteria5);
    criteriaList1.add(criteria6);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
                .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenMultipleGroupCondition_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumnsAndHistory).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStatusCode").condition(OR).operation(FilterOperation.IN).value("LOADED,ACTIVE").valueType(ValueType.STRING).build();
    final SearchCriteria criteria3 = SearchCriteria.builder().key("sisProductName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("MYED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);
    final SearchCriteria criteria4 = SearchCriteria.builder().key("matchedCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria5 = SearchCriteria.builder().key("fixableCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria6 = SearchCriteria.builder().key("sourceStudentCount").condition(OR).operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria4);
    criteriaList1.add(criteria5);
    criteriaList1.add(criteria6);
    final SearchCriteria criteria7 = SearchCriteria.builder().key("studentCount").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.LONG).build();
    final SearchCriteria criteria8 = SearchCriteria.builder().key("sisVendorName").condition(OR).operation(FilterOperation.EQUAL).value("Follett Software").valueType(ValueType.STRING).build();
    final SearchCriteria criteria9 = SearchCriteria.builder().key("mincode").condition(OR).operation(FilterOperation.EQUAL).value("09898027").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList2 = new LinkedList<>();
    criteriaList1.add(criteria7);
    criteriaList1.add(criteria8);
    criteriaList1.add(criteria9);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList2).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch-history/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_HISTORY")))
                .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }

  /**
   * Create sort param string.
   *
   * @return the string
   * @throws JsonProcessingException the json processing exception
   */
  private String createSortParam() throws JsonProcessingException {
    final Map<String, String> sortMap = new LinkedHashMap<>();
    sortMap.put("penRequestBatchEntity.mincode", "DESC");
    sortMap.put("legalLastName", "ASC");
    sortMap.put("legalFirstName", "DESC");
    return new ObjectMapper().writeValueAsString(sortMap);
  }
}
