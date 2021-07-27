package ca.bc.gov.educ.penreg.api.helpers;

import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class LogHelper {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String EXCEPTION = "Exception ";

  private LogHelper() {

  }

  public static void logServerHttpReqResponseDetails(@NonNull final HttpServletRequest request, final HttpServletResponse response) {
    try {
      final int status = response.getStatus();
      val totalTime = Instant.now().toEpochMilli() - (Long) request.getAttribute("startTime");
      final Map<String, Object> httpMap = new HashMap<>();
      httpMap.put("server_http_response_code", status);
      httpMap.put("server_http_request_method", request.getMethod());
      httpMap.put("server_http_query_params", request.getQueryString());
      val correlationID = request.getHeader(ApplicationProperties.CORRELATION_ID);
      if (correlationID != null) {
        httpMap.put("correlation_id", correlationID);
      }
      httpMap.put("server_http_request_url", String.valueOf(request.getRequestURL()));
      httpMap.put("server_http_request_processing_time_ms", totalTime);
      httpMap.put("server_http_request_payload", String.valueOf(request.getAttribute("payload")));
      httpMap.put("server_http_request_remote_address", request.getRemoteAddr());
      httpMap.put("server_http_request_client_name", StringUtils.trimToEmpty(request.getHeader("X-Client-Name")));
      MDC.putCloseable("httpEvent", mapper.writeValueAsString(httpMap));
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

  public static void logClientHttpReqResponseDetails(@NonNull final HttpMethod method, final String url, final int responseCode, final List<String> correlationID) {
    try {
      final Map<String, Object> httpMap = new HashMap<>();
      httpMap.put("client_http_response_code", responseCode);
      httpMap.put("client_http_request_method", method.toString());
      httpMap.put("client_http_request_url", url);
      if (correlationID != null) {
        httpMap.put("correlation_id", String.join(",", correlationID));
      }
      MDC.putCloseable("httpEvent", mapper.writeValueAsString(httpMap));
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

  /**
   * the event is a json string.
   *
   * @param event the json string
   */
  public static void logMessagingEventDetails(final String event) {
    try {
      MDC.putCloseable("messageEvent", event);
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }

  public static void logSagaRetry(final Saga saga) {
    final Map<String, Object> retrySagaMap = new HashMap<>();
    try {
      retrySagaMap.put("sagaName", saga.getSagaName());
      retrySagaMap.put("sagaId", saga.getSagaId());
      retrySagaMap.put("retryCount", saga.getRetryCount());
      MDC.putCloseable("sagaRetry", mapper.writeValueAsString(retrySagaMap));
      log.info("Saga is being retried.");
    } catch (final Exception ex) {
      log.error(EXCEPTION, ex);
    }
  }
}
