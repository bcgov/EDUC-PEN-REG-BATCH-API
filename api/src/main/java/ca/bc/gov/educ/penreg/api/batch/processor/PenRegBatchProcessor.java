package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.batch.exception.FileError;
import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileHeader;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileTrailer;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import net.sf.flatpack.Parser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.batch.constants.BatchFileConstants.*;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The Pen reg batch processor.
 *
 * @author OM
 */
@Component
@Slf4j
public class PenRegBatchProcessor {

  private static final PenRequestBatchFileMapper mapper = PenRequestBatchFileMapper.mapper;
  /**
   * The constant TRANSACTION_CODE_TRAILER_RECORD.
   */
  public static final String TRANSACTION_CODE_TRAILER_RECORD = "BTR";
  /**
   * The constant TRANSACTION_CODE_STUDENT_DETAILS_RECORD.
   */
  public static final String TRANSACTION_CODE_STUDENT_DETAILS_RECORD = "SRM";
  /**
   * The constant TOO_LONG.
   */
  public static final String TOO_LONG = "TOO LONG";
  @Getter(PRIVATE)
  private final PenRequestBatchFileService penRequestBatchFileService;

  /**
   * Instantiates a new Pen reg batch processor.
   *
   * @param penRequestBatchFileService the pen request batch file service
   */
  @Autowired
  public PenRegBatchProcessor(final PenRequestBatchFileService penRequestBatchFileService) {
    this.penRequestBatchFileService = penRequestBatchFileService;
  }

  /**
   * Process pen reg batch file from tsw.
   * 1. <p>The data comes from TSW table so if the the data from the TSW table cant be read error is logged and email is sent.</p>
   * 2. <p>If The data is successfully retrieved from TSW table and file header cant be parsed, system will create only the header record and persist it.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @return the string
   */
  public String processPenRegBatchFileFromTSW(@NonNull PENWebBlobEntity penWebBlobEntity) {
    var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing row from Pen Web Blobs with submission Number :: {} and guid :: {}", penWebBlobEntity.getSubmissionNumber(), guid);
    BatchFile batchFile = new BatchFile();
    Reader batchFileReader = null;
    try (Reader mapperReader = new FileReader(new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mapper.xml")).getFile()))) {

      batchFileReader = new InputStreamReader(new ByteArrayInputStream(penWebBlobEntity.getFileContents()));
      final Parser pzParser = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReader);
      final DataSet ds = pzParser.setNullEmptyStrings(true).parse();
      processDataSetForRowLengthErrors(guid, ds);
      populateBatchFile(guid, ds, batchFile);
      var studentCount = batchFile.getBatchFileTrailer().getStudentCount();
      if (!StringUtils.isNumeric(studentCount) || Integer.parseInt(studentCount) != batchFile.getStudentDetails().size()) {
        throw new FileUnProcessableException(STUDENT_COUNT_MISMATCH, guid, studentCount, String.valueOf(batchFile.getStudentDetails().size()));
      }
      persistData(guid, batchFile, penWebBlobEntity);
    } catch (FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      persistDataWithException(guid, penWebBlobEntity, fileUnProcessableException);
    } catch (Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
    } finally {
      if (batchFileReader != null) {
        try {
          batchFileReader.close();
        } catch (IOException e) {
          log.warn("Error closing the batch file :: " + guid, e);
        }
      }
    }
    return "SUCCESS";
  }

  private void persistDataWithException(@NonNull String guid, @NonNull PENWebBlobEntity penWebBlobEntity, @NonNull FileUnProcessableException fileUnProcessableException) {
    log.info("going to persist data with exception for batch :: {}", guid);
    PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoadFail(penWebBlobEntity, fileUnProcessableException.getReason()); // batch file can be processed further and persisted.
    getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
  }

  private void processDataSetForRowLengthErrors(@NonNull String guid, @NonNull DataSet ds) throws FileUnProcessableException {
    if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
      var message = "";
      for (DataError error : ds.getErrors()) {
        if (error.getErrorDesc() != null && error.getErrorDesc().contains("SHOULD BE 211")) { // Header record should be 211 characters long.
          message = getHeaderRowLengthIncorrectMessage(message, error);
        } else if (error.getErrorDesc() != null && error.getErrorDesc().contains("SHOULD BE 224")) { // Trailer Record should be 224 characters long.
          message = getTrailerRowLengthIncorrectMessage(message, error);
        } else if (error.getErrorDesc() != null && error.getErrorDesc().contains("SHOULD BE 234")) { // Details Record should be 234 characters long.
          message = getDetailRowLengthIncorrectMessage(message, error);
        }
      }
      if (message.length() > 255) {
        message = StringUtils.abbreviate(message, 255);
      }
      throw new FileUnProcessableException(INVALID_ROW_LENGTH, guid, message);
    }
  }

  private String getDetailRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Detail record " + error.getLineNo() + " has extraneous characters, ");
    } else {
      message = message.concat("Detail record " + error.getLineNo() + " is missing characters, ");
    }
    return message;
  }

  private String getTrailerRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Trailer record has extraneous characters, ");
    } else {
      message = message.concat("Trailer record is missing characters, ");
    }
    return message;
  }

  private String getHeaderRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Header record has extraneous characters, ");
    } else {
      message = message.concat("Header record is missing characters, ");
    }
    return message;
  }

  // System was able to process the file successfully, now the data is persisted.
  private void persistData(@NonNull String guid,@NonNull BatchFile batchFile, @NonNull PENWebBlobEntity penWebBlobEntity) {
    log.info("going to persist data for batch :: {}", guid);
    PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoaded(penWebBlobEntity, batchFile); // batch file can be processed further and persisted.
    for (var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      entity.getPenRequestBatchStudentEntities().add(mapper.toPenRequestBatchStudentEntity(student,entity));
    }
    getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
  }



  private void populateBatchFile(String guid, DataSet ds, BatchFile batchFile) throws FileUnProcessableException {
    long index = 0;
    while (ds.next()) {
      if (index == 0 && !ds.isRecordID(HEADER.getName())) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid);
      }
      if (ds.isRecordID(HEADER.getName()) || ds.isRecordID(TRAILER.getName())) {
        setHeaderOrTrailer(ds, batchFile, guid);
        index++;
        continue;
      }
      batchFile.getStudentDetails().add(getStudentDetailRecordFromFile(ds, guid, index));
      index++;
    }
  }

  private StudentDetails getStudentDetailRecordFromFile(final DataSet ds, String guid, long index) throws FileUnProcessableException {
    var transactionCode = ds.getString(TRANSACTION_CODE.getName());
    if (!TRANSACTION_CODE_STUDENT_DETAILS_RECORD.equals(transactionCode)) {
      throw new FileUnProcessableException(INVALID_TRANSACTION_CODE_STUDENT_DETAILS, guid, String.valueOf(index), ds.getString(LOCAL_STUDENT_ID.getName()));
    }
    return StudentDetails.builder()
        .birthDate(ds.getString(BIRTH_DATE.getName()))
        .enrolledGradeCode(ds.getString(ENROLLED_GRADE_CODE.getName()))
        .gender(ds.getString(GENDER.getName()))
        .legalGivenName(ds.getString(LEGAL_GIVEN_NAME.getName()))
        .legalMiddleName(ds.getString(LEGAL_MIDDLE_NAME.getName()))
        .legalSurname(ds.getString(LEGAL_SURNAME.getName()))
        .localStudentID(ds.getString(LOCAL_STUDENT_ID.getName()))
        .pen(ds.getString(PEN.getName()))
        .postalCode(ds.getString(POSTAL_CODE.getName()))
        .transactionCode(transactionCode)
        .unused(ds.getString(UNUSED.getName()))
        .unusedSecond(ds.getString(UNUSED_SECOND.getName()))
        .usualGivenName(ds.getString(USUAL_GIVEN_NAME.getName()))
        .usualMiddleName(ds.getString(USUAL_MIDDLE_NAME.getName()))
        .usualSurname(ds.getString(USUAL_SURNAME.getName()))
        .build();
  }

  private void setHeaderOrTrailer(final DataSet ds, final BatchFile batchFile, String guid) throws FileUnProcessableException {
    if (ds.isRecordID(HEADER.getName())) {
      var minCode = ds.getString(MIN_CODE.getName());
      if (!StringUtils.isNumeric(minCode) || minCode.length() != 8) {
        throw new FileUnProcessableException(INVALID_MINCODE_HEADER, guid);
      }
      batchFile.setBatchFileHeader(BatchFileHeader.builder()
          .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
          .contactName(ds.getString(CONTACT_NAME.getName()))
          .emailID(ds.getString(EMAIL.getName()))
          .faxNumber(ds.getString(FAX_NUMBER.getName()))
          .minCode(minCode)
          .officeNumber(ds.getString(OFFICE_NUMBER.getName()))
          .requestDate(ds.getString(REQUEST_DATE.getName()))
          .schoolName(ds.getString(SCHOOL_NAME.getName()))
          .build());
    } else if (ds.isRecordID(TRAILER.getName())) {
      var transactionCode = ds.getString(TRANSACTION_CODE.getName());
      if (!TRANSACTION_CODE_TRAILER_RECORD.equals(transactionCode)) {
        throw new FileUnProcessableException(INVALID_TRANSACTION_CODE_TRAILER, guid);
      }
      batchFile.setBatchFileTrailer(BatchFileTrailer.builder()
          .transactionCode(transactionCode)
          .productID(ds.getString(PRODUCT_ID.getName()))
          .productName(ds.getString(PRODUCT_NAME.getName()))
          .studentCount(ds.getString(STUDENT_COUNT.getName()))
          .vendorName(ds.getString(VENDOR_NAME.getName()))
          .build());
    }
  }
}
