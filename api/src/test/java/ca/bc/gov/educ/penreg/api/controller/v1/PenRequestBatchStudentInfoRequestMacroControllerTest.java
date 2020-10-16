package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.exception.RestExceptionHandler;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentInfoRequestMacroMapper;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentInfoRequestMacroRepository;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentInfoRequestMacroService;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import ca.bc.gov.educ.penreg.api.support.WithMockOAuth2Scope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The type Pen request batch api controller test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PenRequestBatchStudentInfoRequestMacroControllerTest {
  private static final PenRequestBatchStudentInfoRequestMacroMapper mapper = PenRequestBatchStudentInfoRequestMacroMapper.mapper;
  @Autowired
  PenRequestBatchStudentInfoRequestMacroController controller;

  @Autowired
  PenRequestBatchStudentInfoRequestMacroService service;

  private MockMvc mockMvc;

  @Autowired
  PenRequestBatchStudentInfoRequestMacroRepository repository;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void after() {
    repository.deleteAll();
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH_MACRO")
  public void testRetrievePenRequestBatchMacros_ShouldReturnStatusOK() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch-macro")).andDo(print()).andExpect(status().isOk());
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH_MACRO")
  public void testRetrievePenRequestBatchMacros_GivenInvalidMacroID_ShouldReturnStatusNotFound() throws Exception {
    var result = this.mockMvc.perform(get("/api/v1/pen-request-batch-macro/" + UUID.randomUUID().toString())).andDo(print()).andExpect(status().isNotFound());
    assertThat(result).isNotNull();
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_PEN_REQUEST_BATCH_MACRO")
  public void testRetrievePenRequestBatchMacros_GivenValidMacroID_ShouldReturnStatusOK() throws Exception {
    val entity = mapper.toModel(getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = service.createMacro(entity);
    var result = this.mockMvc.perform(get("/api/v1/pen-request-batch-macro/" + savedEntity.getMacroId().toString())).andDo(print()).andExpect(MockMvcResultMatchers.jsonPath("$.macroId").value(entity.getMacroId().toString())).andExpect(status().isOk()).andReturn();
    assertThat(result).isNotNull();
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_PEN_REQUEST_BATCH_MACRO")
  public void testCreatePenRequestBatchMacros_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-macro").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(dummyPenRequestBatchStudentInfoRequestMacroJson())).andDo(print()).andExpect(status().isCreated());
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_PEN_REQUEST_BATCH_MACRO")
  public void testCreatePenRequestMacros_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-macro").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(dummyPenRequestBatchStudentInfoRequestMacroJsonWithId())).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_PEN_REQUEST_BATCH_MACRO")
  public void testUpdatePenRequestMacros_GivenValidPayload_ShouldReturnStatusOK() throws Exception {
    val entity = mapper.toModel(getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = service.createMacro(entity);
    savedEntity.setCreateDate(null);
    savedEntity.setUpdateDate(null);
    savedEntity.setMacroText("updated text");
    String jsonString = new ObjectMapper().writeValueAsString(mapper.toStructure(savedEntity));
    var result = this.mockMvc.perform(put("/api/v1/pen-request-batch-macro/" + savedEntity.getMacroId().toString()).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(jsonString)).andDo(print()).andExpect(status().isOk());
    assertThat(result).isNotNull();

  }
  @Test
  @WithMockOAuth2Scope(scope = "WRITE_PEN_REQUEST_BATCH_MACRO")
  public void testUpdatePenRequestMacros_GivenInValidPayload_ShouldReturnStatusNotFound() throws Exception {
    val entity = mapper.toModel(getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = service.createMacro(entity);
    savedEntity.setCreateDate(null);
    savedEntity.setUpdateDate(null);
    savedEntity.setMacroText("updated text");
    String jsonString = new ObjectMapper().writeValueAsString(mapper.toStructure(savedEntity));
    var result = this.mockMvc.perform(put("/api/v1/pen-request-batch-macro/" + UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(jsonString)).andDo(print()).andExpect(status().isNotFound());
    assertThat(result).isNotNull();

  }

  protected String dummyPenRequestBatchStudentInfoRequestMacroJson() {
    return " {\n" +
            "    \"createUser\": \"jc\",\n" +
            "    \"updateUser\": \"jc\",\n" +
            "    \"macroCode\": \"hi\",\n" +
            "    \"macroText\": \"top of the morning to ya\"\n" +
            "  }";
  }

  protected String dummyPenRequestBatchStudentInfoRequestMacroJsonWithId() {
    return " {\n" +
            "    \"createUser\": \"jc\",\n" +
            "    \"updateUser\": \"jc\",\n" +
            "    \"macroCode\": \"hi\",\n" +
            "    \"macroId\": \"7f000101-7151-1d84-8171-5187006c0000\",\n" +
            "    \"macroText\": \"well howdy there\"\n" +
            "  }";
  }

  protected PenRequestBatchStudentInfoRequestMacro getPenRequestMacroEntityFromJsonString() {
    try {
      return new ObjectMapper().readValue(dummyPenRequestBatchStudentInfoRequestMacroJson(), PenRequestBatchStudentInfoRequestMacro.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
