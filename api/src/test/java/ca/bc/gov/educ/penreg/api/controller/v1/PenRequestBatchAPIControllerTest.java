package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.batch.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.exception.RestExceptionHandler;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import ca.bc.gov.educ.penreg.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.v1.ValueType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PenRequestBatchAPIControllerTest {
  private MockMvc mockMvc;
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  @Autowired
  PenRequestBatchAPIController penRequestBatchAPIController;
  @Autowired
  PenRequestBatchRepository penRequestBatchRepository;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(penRequestBatchAPIController)
        .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void tearDown() throws Exception {
    penRequestBatchRepository.deleteAll();
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_Always_ShouldReturnStatusOk() throws Exception {
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated")
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenFixableCountFilterWithoutData_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    penRequestBatchRepository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    SearchCriteria criteria = SearchCriteria.builder().key("fixableCount").operation(FilterOperation.GREATER_THAN).value("10").valueType(ValueType.LONG).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatchPaginated_GivenSchoolGroupCodeFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    penRequestBatchRepository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    SearchCriteria criteria = SearchCriteria.builder().key("schoolGroupCode").operation(FilterOperation.EQUAL).value(SchoolGroupCodes.K12.getCode()).valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    MvcResult result = mockMvc
        .perform(get("/api/v1/pen-request-batch/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatch_GivenInvalidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + UUID.randomUUID())).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH")
  public void testReadPenRequestBatch_GivenValidID_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_pen_req_batch.json")).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var results = penRequestBatchRepository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc.perform(get("/api/v1/pen-request-batch/" + results.iterator().next().getPenRequestBatchID())).andDo(print()).andExpect(status().isOk());
  }
}