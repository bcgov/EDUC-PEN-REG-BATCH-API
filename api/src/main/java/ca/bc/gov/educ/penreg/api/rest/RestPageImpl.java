package ca.bc.gov.educ.penreg.api.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Rest page.
 *
 * @param <T> the type parameter
 */
public class RestPageImpl<T> extends PageImpl<T>{

  /**
   * Instantiates a new Rest page.
   *
   * @param content          the content
   * @param number           the number
   * @param size             the size
   * @param totalElements    the total elements
   * @param pageable         the pageable
   * @param last             the last
   * @param totalPages       the total pages
   * @param sort             the sort
   * @param first            the first
   * @param numberOfElements the number of elements
   */
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPageImpl(@JsonProperty("content") List<T> content,
                        @JsonProperty("number") int number,
                        @JsonProperty("size") int size,
                        @JsonProperty("totalElements") Long totalElements,
                        @JsonProperty("pageable") JsonNode pageable,
                        @JsonProperty("last") boolean last,
                        @JsonProperty("totalPages") int totalPages,
                        @JsonProperty("sort") JsonNode sort,
                        @JsonProperty("first") boolean first,
                        @JsonProperty("numberOfElements") int numberOfElements) {

        super(content, PageRequest.of(number, size), totalElements);
    }

  /**
   * Instantiates a new Rest page.
   *
   * @param content  the content
   * @param pageable the pageable
   * @param total    the total
   */
  public RestPageImpl(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

  /**
   * Instantiates a new Rest page.
   *
   * @param content the content
   */
  public RestPageImpl(List<T> content) {
        super(content);
    }

  /**
   * Instantiates a new Rest page.
   */
  public RestPageImpl() {
        super(new ArrayList<>());
    }


}