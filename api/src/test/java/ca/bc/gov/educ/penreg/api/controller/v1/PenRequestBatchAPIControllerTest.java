package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchHistoryRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.penreg.api.struct.v1.Condition.OR;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
public class PenRequestBatchAPIControllerTest {
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
   * The constant student mapper.
   */
  private static final PenRequestBatchStudentMapper studentMapper = PenRequestBatchStudentMapper.mapper;
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
   * The Pen request batch student repository.
   */
  @Autowired
  PenRequestBatchStudentRepository penRequestBatchStudentRepository;
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
    this.penRequestBatchStudentRepository.deleteAll();
    this.penRequestBatchHistoryRepository.deleteAll();
    this.penRequestBatchRepository.deleteAll();
  }

  /**
   * Test read pen request batch paginated always should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_Always_ShouldReturnStatusOk() throws Exception {
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("fixableCount").operation(FilterOperation.GREATER_THAN).value("10").valueType(ValueType.LONG).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
    this.penRequestBatchRepository.saveAll(models);
    final SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
            .param("searchCriteriaList", criteriaJSON)
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
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeOrPenReqBatchStatusCodeFilterAndMatchedCountOrFixableCount_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
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
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
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
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
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
        .perform(get("/api/v1/pen-request-batch/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
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
        .perform(get("/api/v1/pen-request-batch/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
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
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
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
        .perform(get("/api/v1/pen-request-batch/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk());
  }


  /**
   * Test read pen request batch paginated given school name and student name should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_GivenSchoolNameAndStudentName_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("schoolName").operation(FilterOperation.STARTS_WITH).value("Brae").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStudentEntities.legalLastName").condition(AND).operation(FilterOperation.STARTS_WITH).value("JO").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final Map<String, String> sortMap = Map.of(
        "mincode", "ASC",
        "submissionNumber", "ASC"
    );
    final String sorts = objectMapper.writeValueAsString(sortMap);

    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
            .param("searchCriteriaList", criteriaJSON)
            .param("pageNumber", "1")
            .param("pageSize", "1")
            .param("sort", sorts)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  /**
   * Test read pen request batch paginated given school name and student name should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_GivenSchoolNameAndStudentName_WithoutPageAndSortCriteria_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("schoolName").operation(FilterOperation.STARTS_WITH).value("Brae").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStudentEntities.legalLastName").condition(AND).operation(FilterOperation.STARTS_WITH).value("JO").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  /**
   * Test read pen request batch paginated given school name and invalid student name should return empty list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchPaginated_GivenSchoolNameAndInvalidStudentName_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("schoolName").operation(FilterOperation.STARTS_WITH).value("Brae").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStudentEntities.legalLastName").condition(AND).operation(FilterOperation.STARTS_WITH).value("AB").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  /**
   * Test read pen request batch student paginated given multiple group condition should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchStudentPaginated_GivenMultipleGroupCondition_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestBatchStudentStatusCode").operation(FilterOperation.EQUAL).value("LOADED").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("legalFirstName").condition(OR).operation(FilterOperation.STARTS_WITH).value("AC").valueType(ValueType.STRING).build();
    final SearchCriteria criteria3 = SearchCriteria.builder().key("legalLastName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("MI").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);
    final SearchCriteria criteria4 = SearchCriteria.builder().key("penRequestBatchEntity.submissionNumber").operation(FilterOperation.EQUAL).value("T-534093").valueType(ValueType.STRING).build();
    final SearchCriteria criteria5 = SearchCriteria.builder().key("gradeCode").condition(OR).operation(FilterOperation.GREATER_THAN).value("2").valueType(ValueType.STRING).build();
    final SearchCriteria criteria6 = SearchCriteria.builder().key("penRequestBatchEntity.penRequestBatchID").operation(FilterOperation.IN).value(batchIDs).valueType(ValueType.UUID).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria4);
    criteriaList1.add(criteria5);
    criteriaList1.add(criteria6);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final String sort = this.createSortParam();

    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/student/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON).param("sort", sort)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10)));
  }

  /**
   * Test read pen request batch student paginated given pen request batch ids and student status codes should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchStudentPaginated_GivenPenRequestBatchIdsAndStudentStatusCodes_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestBatchEntity.penRequestBatchID").operation(FilterOperation.IN).value(batchIDs).valueType(ValueType.UUID).build();

    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);

    final SearchCriteria criteria1 = SearchCriteria.builder().key("penRequestBatchStudentStatusCode").operation(FilterOperation.IN).value("LOADED,ERROR,FIXABLE").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria1);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final String sort = this.createSortParam();

    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/student/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON).param("sort", sort)
            .param("pageSize", "3")
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  /**
   * Test read pen request batch student paginated given pen request batch ids and all student status codes and other conditions should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchStudentPaginated_GivenPenRequestBatchIdsAndAllStudentStatusCodesAndOtherConditions_ShouldReturnStatusOk() throws Exception {
    final String batchIDs = this.createBatchStudentRecords(2);

    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestBatchEntity.penRequestBatchID").operation(FilterOperation.IN).value(batchIDs).valueType(ValueType.UUID).build();

    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);

    final SearchCriteria criteria2 = SearchCriteria.builder().key("penRequestBatchStudentStatusCode").operation(FilterOperation.NOT_EQUAL).value("FIXABLE").valueType(ValueType.STRING).build();
    final SearchCriteria criteria3 = SearchCriteria.builder().key("penRequestBatchEntity.mincode").condition(AND).operation(FilterOperation.STARTS_WITH_IGNORE_CASE).value("1").valueType(ValueType.STRING).build();
    final SearchCriteria criteria4 = SearchCriteria.builder().key("legalLastName").condition(AND).operation(FilterOperation.STARTS_WITH_IGNORE_CASE).value("j").valueType(ValueType.STRING).build();

    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(criteria2);
    criteriaList1.add(criteria3);
    criteriaList1.add(criteria4);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final String sort = this.createSortParam();

    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/student/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON).param("sort", sort)
            .param("pageSize", "3")
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  /**
   * Test read pen request batch student paginated given multiple group condition 2 should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatchStudentPaginated_GivenMultipleGroupCondition2_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());


    final File student = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch_student.json")).getFile()
    );
    final List<PenRequestBatchStudentEntity> studentEntities = new ObjectMapper().readValue(student, new TypeReference<>() {
    });
    final var students = studentEntities.stream().map(PenRequestBatchUtils::populateAuditColumns).peek(el -> el.setPenRequestBatchEntity(models.get(0))).collect(Collectors.toSet());

    models.get(0).setPenRequestBatchStudentEntities(students);
    this.penRequestBatchRepository.saveAll(models);


    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestBatchStudentStatusCode").operation(FilterOperation.EQUAL).value("LOADED").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("legalFirstName").condition(OR).operation(FilterOperation.STARTS_WITH).value("A").valueType(ValueType.STRING).build();
    final SearchCriteria criteria3 = SearchCriteria.builder().key("legalLastName").condition(OR).operation(FilterOperation.CONTAINS_IGNORE_CASE).value("M").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    criteriaList.add(criteria3);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get("/api/v1/pen-request-batch/student/paginated")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
                .param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(5)));
  }

  /**
   * Test update pen request batch student given pen request batch student should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdatePenRequestBatchStudent_GivenPenRequestBatchStudent_ShouldReturnStatusOk() throws Exception {
    final var models = this.createBatchStudents(1);
    final var student = studentMapper.toStructure(models.get(0).getPenRequestBatchStudentEntities().stream().findFirst().orElseThrow());

    student.setPenRequestBatchStudentStatusCode("FIXABLE");
    student.setInfoRequest("Test Info");
    final var request = new ObjectMapper().writeValueAsString(student);

    this.mockMvc
        .perform(put(String.format("/api/v1/pen-request-batch/%s/student/%s", student.getPenRequestBatchID(), student.getPenRequestBatchStudentID()))
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH")))
            .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).content(request))
        .andDo(print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.penRequestBatchStudentStatusCode", is("FIXABLE")))
        .andExpect(jsonPath("$.infoRequest", is("Test Info")));
  }

  /**
   * Test read pen request batch given invalid id should return status not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatch_GivenInvalidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + UUID.randomUUID())
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH"))))
            .andDo(print()).andExpect(status().isNotFound());
  }

  /**
   * Test read pen request batch given valid id should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadPenRequestBatch_GivenValidID_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(Collectors.toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(Collectors.toList());
    final var results = this.penRequestBatchRepository.saveAll(models);
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + results.iterator().next().getPenRequestBatchID())
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH"))))
            .andDo(print()).andExpect(status().isOk());
  }

  /**
   * Test update pen request batch given pen request batch id should return status ok.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdatePenRequestBatch_GivenPenRequestBatchId_ShouldReturnStatusOk() throws Exception {
    final var models = this.createBatchStudents(1);
    final var batch = mapper.toStructure(models.get(0));

    batch.setPenRequestBatchStatusCode("ARCHIVED");
    final var request = new ObjectMapper().writeValueAsString(batch);

    this.mockMvc
        .perform(put(String.format("/api/v1/pen-request-batch/%s", batch.getPenRequestBatchID()))
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH")))
            .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).content(request))
        .andDo(print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.penRequestBatchStatusCode", is("ARCHIVED")));
  }

  @Test
  public void testStats_givenDataINDB_ShouldReturnStatusOkWithData() throws Exception {
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("API_PEN_REQUEST_BATCH_PEN_REQUEST_BATCH.json")).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().peek(x -> {
      x.setInsertDate(LocalDateTime.now().toString());
      x.setExtractDate(LocalDateTime.now().toString());
    }).map(PenRequestBatchMapper.mapper::toModel).collect(toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(toList());

    this.penRequestBatchRepository.saveAll(models);

    this.mockMvc
        .perform(get("/api/v1/pen-request-batch/stats")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH")))
            .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
        .andDo(print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.loadFailCount", is(6)));
  }

  /**
   * Create batch students list.
   *
   * @param total the total
   * @return the list
   * @throws IOException the io exception
   */
  private List<PenRequestBatchEntity> createBatchStudents(final Integer total) throws IOException {
    return PenRequestBatchUtils.createBatchStudents(this.penRequestBatchRepository, "mock_pen_req_batch.json",
        "mock_pen_req_batch_student.json", total);
  }

  /**
   * Create batch student records string.
   *
   * @param total the total
   * @return the string
   * @throws IOException the io exception
   */
  private String createBatchStudentRecords(final Integer total) throws IOException {

    final var models = this.createBatchStudents(total);

    return models.stream().map(batch -> batch.getPenRequestBatchID().toString().toUpperCase()).collect(Collectors.joining(","));
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
