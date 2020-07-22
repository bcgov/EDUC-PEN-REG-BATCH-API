package ca.bc.gov.educ.api.student.controller;

import ca.bc.gov.educ.api.student.StudentApiApplication;
import ca.bc.gov.educ.api.student.exception.RestExceptionHandler;
import ca.bc.gov.educ.api.student.filter.FilterOperation;
import ca.bc.gov.educ.api.student.mappers.StudentMapper;
import ca.bc.gov.educ.api.student.model.*;
import ca.bc.gov.educ.api.student.repository.*;
import ca.bc.gov.educ.api.student.service.StudentService;
import ca.bc.gov.educ.api.student.struct.SearchCriteria;
import ca.bc.gov.educ.api.student.struct.Student;
import ca.bc.gov.educ.api.student.struct.ValueType;
import ca.bc.gov.educ.api.student.support.WithMockOAuth2Scope;
import ca.bc.gov.educ.api.student.validator.StudentPayloadValidator;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.time.LocalDate;
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
@SpringBootTest(classes = StudentApiApplication.class)
public class PenRegAPIControllerTest {

  private static final StudentMapper mapper = StudentMapper.mapper;
  private MockMvc mockMvc;
  @Autowired
  StudentController controller;

  @Autowired
  StudentRepository repository;
  
  @Autowired
  GenderCodeTableRepository genderRepo;
  
  @Autowired
  SexCodeTableRepository sexRepo;

  @Autowired
  DemogCodeTableRepository demogRepo;

  @Autowired
  StatusCodeTableRepository statusRepo;

  @Autowired
  GradeCodeTableRepository gradeRepo;

  @Autowired
  StudentPayloadValidator validator;
  
  @Autowired
  StudentService studentService;


  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler()).build();
    genderRepo.save(createGenderCodeData());
    sexRepo.save(createSexCodeData());
    demogRepo.save(createDemogCodeData());
    statusRepo.save(createStatusCodeData());
    gradeRepo.save(createGradeCodeData());
  }
  
  /**
   * need to delete the records to make it working in unit tests assertion, else the records will keep growing and assertions will fail.
   */
  @After
  public void after() {
    genderRepo.deleteAll();
    sexRepo.deleteAll();
    demogRepo.deleteAll();
    statusRepo.deleteAll();
    gradeRepo.deleteAll();
    repository.deleteAll();
  }

  private SexCodeEntity createSexCodeData() {
    return SexCodeEntity.builder().sexCode("M").description("Male")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("label").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  private GenderCodeEntity createGenderCodeData() {
    return GenderCodeEntity.builder().genderCode("M").description("Male")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("label").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  private StatusCodeEntity createStatusCodeData() {
    return StatusCodeEntity.builder().statusCode("A").description("Active")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("label").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  private DemogCodeEntity createDemogCodeData() {
    return DemogCodeEntity.builder().demogCode("A").description("Accepted")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("label").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  private GradeCodeEntity createGradeCodeData() {
    return GradeCodeEntity.builder().gradeCode("01").description("First Grade")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("label").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }


  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testRetrieveStudent_GivenRandomID_ShouldThrowEntityNotFoundException() throws Exception {
    this.mockMvc.perform(get("/" + UUID.randomUUID())).andDo(print()).andExpect(status().isNotFound());
  }


  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testRetrieveStudent_GivenValidID_ShouldReturnStatusOK() throws Exception {
    StudentEntity entity = repository.save(createStudent());
    this.mockMvc.perform(get("/" + entity.getStudentID())).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.studentID").value(entity.getStudentID().toString()));
  }
  

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testRetrieveStudent_GivenPEN_ShouldReturnStatusOK() throws Exception {
    StudentEntity entity = repository.save(createStudent());
    this.mockMvc.perform(get("/?pen=" + entity.getPen())).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$[0].studentID").value(entity.getStudentID().toString()));
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_STUDENT")
  public void testCreateStudent_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    StudentEntity entity = createStudent();
    this.mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(StudentMapper.mapper.toStructure(entity)))).andDo(print()).andExpect(status().isCreated());
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_STUDENT")
  public void testCreateStudent_GivenInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
	StudentEntity entity = createStudent();
	entity.setSexCode("J");
    this.mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(StudentMapper.mapper.toStructure(entity)))).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockOAuth2Scope(scope = "WRITE_STUDENT")
  public void testUpdateStudent_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    StudentEntity entity = createStudent();
    repository.save(entity);
    entity.setLegalFirstName("updated");
    this.mockMvc.perform(put("/").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(StudentMapper.mapper.toStructure(entity)))).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.legalFirstName").value(entity.getLegalFirstName()));
  }

  @Test
  @WithMockOAuth2Scope(scope = "DELETE_STUDENT")
  public void testDeleteStudent_GivenValidId_ShouldReturnStatus204() throws Exception {
    StudentEntity entity = createStudent();
    repository.save(entity);
    this.mockMvc.perform(delete("/"+entity.getStudentID().toString()).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isNoContent());
  }

  @Test
  @WithMockOAuth2Scope(scope = "DELETE_STUDENT")
  public void testDeleteStudent_GivenInvalidId_ShouldReturnStatus404() throws Exception {
    this.mockMvc.perform(delete("/"+UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_Always_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated?pageSize=2")
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_whenNoDataInDB_ShouldReturnStatusOk() throws Exception {
    MvcResult result = mockMvc
            .perform(get("/paginated")
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }
  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginatedWithSorting_Always_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    Map<String, String> sortMap = new HashMap<>();
    sortMap.put("legalLastName", "ASC");
    sortMap.put("legalFirstName", "DESC");
    String sort = new ObjectMapper().writeValueAsString(sortMap);
    MvcResult result = mockMvc
            .perform(get("/paginated").param("pageNumber","1").param("pageSize", "5").param("sort", sort)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_GivenFirstNameFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalFirstName").operation(FilterOperation.EQUAL).value("Leonor").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_GivenLastNameFilter_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.EQUAL).value("Warner").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_GivenSubmitDateBetween_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    String fromDate = "2017-04-01";
    String toDate = "2018-04-15";
    SearchCriteria criteria = SearchCriteria.builder().key("dob").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.DATE).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_GivenFirstAndLast_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    String fromDate = "1990-04-01";
    String toDate = "2020-04-15";
    SearchCriteria criteria = SearchCriteria.builder().key("dob").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.DATE).build();
    SearchCriteria criteriaFirstName = SearchCriteria.builder().key("legalFirstName").operation(FilterOperation.CONTAINS).value("a").valueType(ValueType.STRING).build();
    SearchCriteria criteriaLastName = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.CONTAINS).value("l").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteriaFirstName);
    criteriaList.add(criteriaLastName);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_LegalLastNameFilterIgnoreCase_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.CONTAINS_IGNORE_CASE).value("b").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
            .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_LegalLastNameStartWith_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.STARTS_WITH).value("Ham").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
        .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_LegalLastNameStartWith2_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.STARTS_WITH).value("ham").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
        .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }
  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_LegalLastNameStartWithIgnoreCase_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    SearchCriteria criteria = SearchCriteria.builder().key("legalLastName").operation(FilterOperation.STARTS_WITH_IGNORE_CASE).value("ham").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    System.out.println(criteriaJSON);
    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    MvcResult result = mockMvc
        .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }
  @Test
  @WithMockOAuth2Scope(scope = "READ_STUDENT")
  public void testReadStudentPaginated_LegalLastNameStartWithIgnoreCase2_ShouldReturnStatusOk() throws Exception {
    final File file = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mock_students.json")).getFile()
    );
    List<Student> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    val entitiesFromDB = repository.findAll();
    SearchCriteria criteria = SearchCriteria.builder().key("studentID").operation(FilterOperation.EQUAL).value(entitiesFromDB.get(0).getStudentID().toString()).valueType(ValueType.UUID).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
    MvcResult result = mockMvc
        .perform(get("/paginated").param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  private StudentEntity createStudent() {
    StudentEntity student = new StudentEntity();
    student.setPen("987654321");
    student.setLegalFirstName("John");
    student.setLegalMiddleNames("Duke");
    student.setLegalLastName("Wayne");
    student.setDob(LocalDate.parse("1907-05-26"));
    student.setSexCode("M");
    student.setUsualFirstName("Johnny");
    student.setUsualMiddleNames("Duke");
    student.setUsualLastName("Wayne");
    student.setEmail("theduke@someplace.com");
    student.setEmailVerified("Y");
    student.setDeceasedDate(LocalDate.parse("1979-06-11"));
    return student;
  }

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
