package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.Search;
import ca.bc.gov.educ.penreg.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.penreg.api.struct.v1.ValueType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {

  private static final String PARAMETERS_ATTRIBUTE = "parameters";

  private final ApplicationProperties props;

  public RestUtils(@Autowired final ApplicationProperties props) {
    this.props = props;
  }

  public RestTemplate getRestTemplate() {
    return getRestTemplate(null);
  }

  public RestTemplate getRestTemplate(List<String> scopes) {
    log.debug("Calling get token method");
    ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
    resourceDetails.setClientId(props.getClientID());
    resourceDetails.setClientSecret(props.getClientSecret());
    resourceDetails.setAccessTokenUri(props.getTokenURL());
    if (scopes != null) {
      resourceDetails.setScope(scopes);
    }
    return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());
  }

  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student getStudentByStudentID(String studentID) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(props.getStudentApiURL() + "/" + studentID, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), Student.class).getBody();
  }

  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateStudent(Student studentFromStudentAPI) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    restTemplate.exchange(props.getStudentApiURL(), HttpMethod.PUT, new HttpEntity<>(studentFromStudentAPI, headers), Student.class);
  }

  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public int getLatestPenNumberFromStudentAPI(String guid) throws JsonProcessingException {
    RestTemplate restTemplate = getRestTemplate();
    SearchCriteria criteria = SearchCriteria.builder().key("pen").operation(FilterOperation.STARTS_WITH).value("1").valueType(ValueType.STRING).build();
    List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(props.getStudentApiURL() + "/paginated")
        .queryParam("searchCriteriaList", criteriaJSON)
        .queryParam("pageSize", 1)
        .queryParam("sort", "{\"pen\":\"DESC\"}");

    DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
    defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
    restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

    ParameterizedTypeReference<RestPageImpl<Student>> responseType = new ParameterizedTypeReference<>() {
    };
    var url = builder.toUriString();
    log.info("url is :: {}", url);
    ResponseEntity<RestPageImpl<Student>> studentResponse = restTemplate.exchange(url, HttpMethod.GET, null, responseType);

    var optionalStudent = Objects.requireNonNull(studentResponse.getBody()).getContent().stream().findFirst();
    if (optionalStudent.isPresent()) {
      var firstStudent = optionalStudent.get();
      return Integer.parseInt(firstStudent.getPen().substring(0, 8));
    }
    log.warn("PEN could not be retrieved, returning 0 for guid :: {}", guid);
    return 0;
  }

  @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student createStudent(Student student) {
    RestTemplate restTemplate = getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(props.getStudentApiURL(), HttpMethod.POST, new HttpEntity<>(student, headers), Student.class).getBody();
  }
}
