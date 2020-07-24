package ca.bc.gov.educ.penreg.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The type Json util.
 *  @author OM
 */
public class JsonUtil {
  private JsonUtil(){
  }

  /**
   * Gets json string from object.
   *
   * @param payload the payload
   * @return the json string from object
   * @throws JsonProcessingException the json processing exception
   */
  public static String getJsonStringFromObject(Object payload) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(payload);
  }

  /**
   * Gets json object from string.
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the payload
   * @return the json object from string
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getJsonObjectFromString(Class<T> clazz,  String payload) throws JsonProcessingException {
    return new ObjectMapper().readValue(payload,clazz);
  }
}
