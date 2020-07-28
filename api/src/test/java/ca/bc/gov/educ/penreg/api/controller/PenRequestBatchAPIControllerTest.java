package ca.bc.gov.educ.penreg.api.controller;

import ca.bc.gov.educ.penreg.api.exception.RestExceptionHandler;
import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.mappers.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.struct.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.ValueType;
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
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class PenRequestBatchAPIControllerTest  {

  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;
  private MockMvc mockMvc;
  @Autowired
  PenRequestBatchAPIController controller;

  @Autowired
  PenRequestBatchRepository repository;

  @Autowired
  PenRequestBatchStudentRepository studentRepository;
  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void after() {
    repository.deleteAll();
  }




}
