package ca.bc.gov.educ.penreg.api.batch.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUnProcessableException extends Exception {

  public static final String GUID_IS = " guid is :: ";
  @Getter
  private final String reason;
  public FileUnProcessableException(FileError fileError, String guid, String... messageArgs) {
    super(fileError.getMessage() + GUID_IS + guid);
    var finalLogMessage = fileError.getMessage();
    if (messageArgs != null) {
     finalLogMessage = getFormattedMessage(finalLogMessage, messageArgs);
    }
    log.error(finalLogMessage + GUID_IS + guid);
    reason = finalLogMessage;
  }
  private static String getFormattedMessage(String msg, String... substitutions) {
    final String format = msg.replaceAll("\\Q$?\\E", "%s");
    return String.format(format, (Object[]) substitutions);
  }
}
