package ca.bc.gov.educ.penreg.api.batch.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The type File un processable exception.
 */
@Slf4j
public class FileUnProcessableException extends Exception {

  /**
   * The constant GUID_IS.
   */
  public static final String GUID_IS = " guid is :: ";
  @Getter
  private final FileError fileError;
  @Getter
  private final String reason;

  /**
   * Instantiates a new File un processable exception.
   *
   * @param fileError   the file error
   * @param guid        the guid
   * @param messageArgs the message args
   */
  public FileUnProcessableException(FileError fileError, String guid, String... messageArgs) {
    super(fileError.getMessage() + GUID_IS + guid);
    this.fileError = fileError;
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
