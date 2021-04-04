package ca.bc.gov.educ.penreg.api.controller.v1;

import ca.bc.gov.educ.penreg.api.BaseTest;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchStudentInfoRequestMacroMapper;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentInfoRequestMacroRepository;
import ca.bc.gov.educ.penreg.api.service.PenRequestBatchStudentInfoRequestMacroService;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudentInfoRequestMacro;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The type Pen request batch api controller test.
 */
public class PenRequestBatchStudentInfoRequestMacroControllerTest extends BaseTest {
  private static final PenRequestBatchStudentInfoRequestMacroMapper mapper = PenRequestBatchStudentInfoRequestMacroMapper.mapper;
  @Autowired
  PenRequestBatchStudentInfoRequestMacroController controller;

  @Autowired
  PenRequestBatchStudentInfoRequestMacroService service;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  PenRequestBatchStudentInfoRequestMacroRepository repository;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }


  @Test
  public void testRetrievePenRequestBatchMacros_ShouldReturnStatusOK() throws Exception {
    this.mockMvc.perform(get("/api/v1/pen-request-batch-macro")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_MACRO"))))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void testRetrievePenRequestBatchMacros_GivenInvalidMacroID_ShouldReturnStatusNotFound() throws Exception {
    final var result = this.mockMvc.perform(get("/api/v1/pen-request-batch-macro/" + UUID.randomUUID().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_MACRO"))))
        .andDo(print()).andExpect(status().isNotFound());
    assertThat(result).isNotNull();
  }

  @Test
  public void testRetrievePenRequestBatchMacros_GivenValidMacroID_ShouldReturnStatusOK() throws Exception {
    val entity = mapper.toModel(this.getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = this.service.createMacro(entity);
    final var result = this.mockMvc.perform(get("/api/v1/pen-request-batch-macro/" + savedEntity.getMacroId().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_PEN_REQUEST_BATCH_MACRO"))))
        .andDo(print()).andExpect(MockMvcResultMatchers.jsonPath("$.macroId").value(entity.getMacroId().toString())).andExpect(status().isOk()).andReturn();
    assertThat(result).isNotNull();
  }

  @Test
  public void testCreatePenRequestBatchMacros_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-macro")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH_MACRO")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON).content(this.dummyPenRequestBatchStudentInfoRequestMacroJson())).andDo(print()).andExpect(status().isCreated());
  }

  @Test
  public void testCreatePenRequestMacros_GivenInValidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post("/api/v1/pen-request-batch-macro")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH_MACRO")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON).content(this.dummyPenRequestBatchStudentInfoRequestMacroJsonWithId())).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testUpdatePenRequestMacros_GivenValidPayload_ShouldReturnStatusOK() throws Exception {
    val entity = mapper.toModel(this.getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = this.service.createMacro(entity);
    savedEntity.setCreateDate(null);
    savedEntity.setUpdateDate(null);
    savedEntity.setMacroText("updated text");
    final String jsonString = new ObjectMapper().writeValueAsString(mapper.toStructure(savedEntity));
    final var result = this.mockMvc.perform(put("/api/v1/pen-request-batch-macro/" + savedEntity.getMacroId().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH_MACRO")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON).content(jsonString)).andDo(print()).andExpect(status().isOk());
    assertThat(result).isNotNull();

  }
  @Test
  public void testUpdatePenRequestMacros_GivenInValidPayload_ShouldReturnStatusNotFound() throws Exception {
    val entity = mapper.toModel(this.getPenRequestMacroEntityFromJsonString());
    entity.setMacroId(null);
    entity.setCreateDate(LocalDateTime.now());
    entity.setUpdateDate(LocalDateTime.now());
    val savedEntity = this.service.createMacro(entity);
    savedEntity.setCreateDate(null);
    savedEntity.setUpdateDate(null);
    savedEntity.setMacroText("updated text");
    final String jsonString = new ObjectMapper().writeValueAsString(mapper.toStructure(savedEntity));
    final var result = this.mockMvc.perform(put("/api/v1/pen-request-batch-macro/" + UUID.randomUUID().toString())
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_PEN_REQUEST_BATCH_MACRO")))
        .contentType(MediaType.APPLICATION_JSON)
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
      return new ObjectMapper().readValue(this.dummyPenRequestBatchStudentInfoRequestMacroJson(), PenRequestBatchStudentInfoRequestMacro.class);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
