package ca.bc.gov.educ.penreg.api.batch.validator;

import ca.bc.gov.educ.penreg.api.batch.exception.FileError;
import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.School;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.*;

/**
 * this class is responsible to validate the batch file that was uploaded.
 */
@Component
@Slf4j
public class PenRequestBatchFileValidator {

  public static final String HEADER_STARTS_WITH = "FFI";
  public static final String TRAILER_STARTS_WITH = "BTR";
  /**
   * The constant TOO_LONG.
   */
  public static final String TOO_LONG = "TOO LONG";
  private final RestUtils restUtils;
  private final ApplicationProperties applicationProperties;

  @Autowired
  public PenRequestBatchFileValidator(final RestUtils restUtils, final ApplicationProperties applicationProperties) {
    this.restUtils = restUtils;
    this.applicationProperties = applicationProperties;
  }

  public void validateFileForFormatAndLength(@NonNull final String guid, @NonNull final DataSet ds) throws FileUnProcessableException {
    if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
      this.validateHeaderWhenFileHasLengthErrors(guid, ds);
      this.validateTrailerWhenFileHasLengthErrors(guid, ds);
    } else {
      this.validateHeaderTrailerWhenFileHasNoLengthErrors(guid, ds);
    }
    this.processDataSetForRowLengthErrors(guid, ds);
  }

  private void validateTrailerWhenFileHasLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    final int totalRecords = ds.getRowCount() + ds.getErrorCount();
    final Optional<DataError> isErrorOnLastLineOptional = ds.getErrors().stream().filter(el -> el.getLineNo() == totalRecords).findFirst();
    if (isErrorOnLastLineOptional.isPresent()) {
      if (!StringUtils.startsWith(isErrorOnLastLineOptional.get().getRawData(), TRAILER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
    } else {
      ds.goBottom();
      if (!StringUtils.startsWith(ds.getRawData(), TRAILER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
      ds.goTop(); // reset and move the cursor to top as everything is fine.
    }
  }

  private void validateHeaderWhenFileHasLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    final Optional<DataError> isErrorOnFirstLineOptional = ds.getErrors().stream().filter(el -> el.getLineNo() == 1).findFirst();
    if (isErrorOnFirstLineOptional.isPresent()) {
      if (!StringUtils.startsWith(isErrorOnFirstLineOptional.get().getRawData(), HEADER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
    } else {
      ds.goTop();
      ds.next();
      if (!StringUtils.startsWith(ds.getRawData(), HEADER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
    }
  }

  private void validateHeaderTrailerWhenFileHasNoLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    ds.goTop();
    ds.next();
    if (!StringUtils.startsWith(ds.getRawData(), HEADER_STARTS_WITH)) {
      throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }
    ds.goBottom();
    if (!StringUtils.startsWith(ds.getRawData(), TRAILER_STARTS_WITH)) {
      throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }
    ds.goTop(); // reset and move the cursor to top as everything is fine.
  }

  /**
   * Process data set for row length errors.
   *
   * @param guid the guid
   * @param ds   the ds
   * @throws FileUnProcessableException the file un processable exception
   */
  private void processDataSetForRowLengthErrors(@NonNull final String guid, @NonNull final DataSet ds) throws FileUnProcessableException {
    if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
      var message = "";
      var firstErrorFound = false;
      for (final DataError error : ds.getErrors()) {
        // ignore the header error to allow all flavours of header
        if (error.getErrorDesc() != null && error.getErrorDesc().contains("SHOULD BE 235")) { // Details Record should be 235 characters long.
          message = this.getDetailRowLengthIncorrectMessage(message, error);
          firstErrorFound = true;
        }
        // ignore the footer error to allow all flavours of footer
        if (firstErrorFound) {
          break; // if system found one error , system breaks the loop.
        }
      }
      if(firstErrorFound) {
        throw new FileUnProcessableException(INVALID_ROW_LENGTH, guid, PenRequestBatchStatusCodes.LOAD_FAIL, message);
      }
    }
  }


  /**
   * Gets detail row length incorrect message.
   * here 1 is subtracted from the line number as line number starts from header record and here header record
   * needs to
   * be  discarded
   *
   * @param message the message
   * @param error   the error
   * @return the detail row length incorrect message
   */
  private String getDetailRowLengthIncorrectMessage(String message, final DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Detail record " + (error.getLineNo() - 1) + " has extraneous characters.");
    } else {
      message = message.concat("Detail record " + (error.getLineNo() - 1) + " is missing characters.");
    }
    return message;
  }

  /**
   * this method validates the min code. it calls out to school api and if there is no data in school api for the mincode then it fails
   *
   * @param guid    the guid
   * @param mincode the min code
   * @throws FileUnProcessableException the file un processable exception
   */
  public void validateMincode(final String guid, final String mincode) throws FileUnProcessableException {
    if (!StringUtils.isNumeric(mincode) || mincode.length() != 8) {
      throw new FileUnProcessableException(INVALID_MINCODE_HEADER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }
    try {
      final Optional<School> school = this.restUtils.getSchoolByMincode(mincode);
      if (school.isEmpty()) {
        throw new FileUnProcessableException(INVALID_MINCODE_HEADER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
      final String openedDate = school.get().getDateOpened();
      final String closedDate = school.get().getDateClosed();
      if (openedDate == null || LocalDate.parse(openedDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME).isAfter(LocalDate.now()) || (closedDate != null && LocalDate.parse(closedDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME).isBefore(LocalDate.now()))) {
        throw new FileUnProcessableException(INVALID_MINCODE_SCHOOL_CLOSED, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
      }
    } catch (final DateTimeParseException e) {
      log.error("Date time parse exception trying to parse School's open or closed date: {}", guid, e);
      throw new FileUnProcessableException(INVALID_MINCODE_SCHOOL_CLOSED, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }
  }

  /**
   * Process student count for mismatch and size.
   *
   * @param guid      the guid
   * @param batchFile the batch file
   * @throws FileUnProcessableException the file un processable exception
   */
  public void validateStudentCountForMismatchAndSize(final String guid, final BatchFile batchFile) throws FileUnProcessableException {
    final var studentCount = batchFile.getBatchFileTrailer().getStudentCount();
    if (!StringUtils.isNumeric(studentCount) || Integer.parseInt(studentCount) != batchFile.getStudentDetails().size()) {
      throw new FileUnProcessableException(STUDENT_COUNT_MISMATCH, guid, PenRequestBatchStatusCodes.LOAD_FAIL, studentCount, String.valueOf(batchFile.getStudentDetails().size()));
    } else if (batchFile.getStudentDetails().size() >= this.applicationProperties.getNumRecordsForBatchHold()) {
      throw new FileUnProcessableException(HELD_BACK_FOR_SIZE, guid, PenRequestBatchStatusCodes.HOLD_SIZE);
    }
  }
}
