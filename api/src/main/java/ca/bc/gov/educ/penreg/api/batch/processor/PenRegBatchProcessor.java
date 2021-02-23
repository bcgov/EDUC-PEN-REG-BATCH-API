package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.batch.exception.FileError;
import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchStudentSagaDataMapper;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileHeader;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileTrailer;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.model.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.PenRequestBatchStudentSagaData;
import ca.bc.gov.educ.penreg.api.struct.School;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.*;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.*;
import static java.time.temporal.ChronoField.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The Pen reg batch processor.
 *
 * @author OM
 */
@Component
@Slf4j
public class PenRegBatchProcessor {

  /**
   * The constant mapper.
   */
  private static final PenRequestBatchFileMapper mapper = PenRequestBatchFileMapper.mapper;
  /**
   * The constant studentSagaDataMapper.
   */
  private static final PenRequestBatchStudentSagaDataMapper studentSagaDataMapper = PenRequestBatchStudentSagaDataMapper.mapper;
  /**
   * The Pen reg batch student records processor.
   */
  @Getter(PRIVATE)
  private final PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor;
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
  /**
   * The Pen request batch file service.
   */
  @Getter(PRIVATE)
  private final PenRequestBatchFileService penRequestBatchFileService;

  /**
   * The Application properties.
   */
  @Getter
  private final ApplicationProperties applicationProperties;

  /**
   * The Rest utils.
   */
  private final RestUtils restUtils;

  private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendValue(YEAR, 4)
          .appendValue(MONTH_OF_YEAR, 2)
          .appendValue(DAY_OF_MONTH, 2).toFormatter();

  /**
   * Instantiates a new Pen reg batch processor.
   *
   * @param penRegBatchStudentRecordsProcessor the pen reg batch student records processor
   * @param penRequestBatchFileService         the pen request batch file service
   * @param restUtils                          the rest utils
   */
  @Autowired
  public PenRegBatchProcessor(PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor, final PenRequestBatchFileService penRequestBatchFileService, RestUtils restUtils, ApplicationProperties applicationProperties) {
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
    this.penRequestBatchFileService = penRequestBatchFileService;
    this.restUtils = restUtils;
    this.applicationProperties = applicationProperties;
  }

  /**
   * Process pen reg batch file from tsw.
   * 1. <p>The data comes from TSW table so if the the data from the TSW table cant be read error is logged and email is sent.</p>
   * 2. <p>If The data is successfully retrieved from TSW table and file header cant be parsed, system will create only the header record and persist it.
   *
   * @param penWebBlobEntity the pen web blob entity
   */
  @Transactional
  public void processPenRegBatchFileFromPenWebBlob(@NonNull final PENWebBlobEntity penWebBlobEntity) {
    var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing row from Pen Web Blobs with submission Number :: {} and guid :: {}", penWebBlobEntity.getSubmissionNumber(), guid);
    BatchFile batchFile = new BatchFile();
    Optional<Reader> batchFileReaderOptional = Optional.empty();
    try (Reader mapperReader = new FileReader(Objects.requireNonNull(getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(penWebBlobEntity.getFileContents())));
      final Parser pzParser = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get());
      final DataSet ds = pzParser.setNullEmptyStrings(true).parse();
      processDataSetForRowLengthErrors(guid, ds);
      populateBatchFile(guid, ds, batchFile);
      var studentCount = batchFile.getBatchFileTrailer().getStudentCount();
      if (!StringUtils.isNumeric(studentCount) || Integer.parseInt(studentCount) != batchFile.getStudentDetails().size()) {
        throw new FileUnProcessableException(STUDENT_COUNT_MISMATCH, guid, studentCount, String.valueOf(batchFile.getStudentDetails().size()));
      }

      //Running check for large batch file
      if (batchFile.getStudentDetails().size() >= getApplicationProperties().getNumRecordsForBatchHold()) {
        persistDataForHeldBackBatchOnSize(guid, penWebBlobEntity, batchFile);
      }else {
        var studentSagaDataSet = processLoadedRecordsInBatchFile(guid, batchFile, penWebBlobEntity);
        getPenRegBatchStudentRecordsProcessor().publishUnprocessedStudentRecordsForProcessing(studentSagaDataSet);
      }
    } catch (FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      persistDataWithException(guid, penWebBlobEntity, fileUnProcessableException);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
    } finally {
      if (batchFileReaderOptional.isPresent()) {
        try {
          batchFileReaderOptional.get().close();
        } catch (final IOException e) {
          log.warn("Error closing the batch file :: " + guid, e);
        }
      }
    }
  }

  /**
   * Set batch to LOAD_HELD_SIZE status.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   */
  private void persistDataForHeldBackBatchOnSize(@NonNull String guid, @NonNull PENWebBlobEntity penWebBlobEntity, BatchFile file) {
    log.info("going to persist data for a batch held back for size :: {}", guid);
    PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoadHeldForSize(penWebBlobEntity, file); // batch file can be processed further and persisted.
    getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
  }

  /**
   * Persist data with exception.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   * @param fileUnProcessableException the file un processable exception
   */
  private void persistDataWithException(@NonNull String guid, @NonNull PENWebBlobEntity penWebBlobEntity, @NonNull FileUnProcessableException fileUnProcessableException) {
    log.info("going to persist data with exception for batch :: {}", guid);
    PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoadFail(penWebBlobEntity, fileUnProcessableException.getReason()); // batch file can be processed further and persisted.
    getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
  }

  /**
   * Process data set for row length errors.
   *
   * @param guid the guid
   * @param ds   the ds
   * @throws FileUnProcessableException the file un processable exception
   */
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

  /**
   * Gets detail row length incorrect message.
   *
   * @param message the message
   * @param error   the error
   * @return the detail row length incorrect message
   */
  private String getDetailRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Detail record " + error.getLineNo() + " has extraneous characters, ");
    } else {
      message = message.concat("Detail record " + error.getLineNo() + " is missing characters, ");
    }
    return message;
  }

  /**
   * Gets trailer row length incorrect message.
   *
   * @param message the message
   * @param error   the error
   * @return the trailer row length incorrect message
   */
  private String getTrailerRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Trailer record has extraneous characters, ");
    } else {
      message = message.concat("Trailer record is missing characters, ");
    }
    return message;
  }

  /**
   * Gets header row length incorrect message.
   *
   * @param message the message
   * @param error   the error
   * @return the header row length incorrect message
   */
  private String getHeaderRowLengthIncorrectMessage(String message, DataError error) {
    if (error.getErrorDesc().contains(TOO_LONG)) {
      message = message.concat("Header record has extraneous characters, ");
    } else {
      message = message.concat("Header record is missing characters, ");
    }
    return message;
  }

  /**
   * System was able to process the file successfully, now the data is persisted and saga data is created for further processing.
   * Process loaded records in batch file set.
   * this method will convert from batch file to header and student record,
   * send them to service for persistence and then return the set for further processing.
   *
   * @param guid             the guid
   * @param batchFile        the batch file
   * @param penWebBlobEntity the pen web blob entity
   * @return set {@link PenRequestBatchStudentSagaData}
   */
  private Set<PenRequestBatchStudentSagaData> processLoadedRecordsInBatchFile(@NonNull String guid, @NonNull BatchFile batchFile, @NonNull PENWebBlobEntity penWebBlobEntity) {
    var counter = 1;
    log.info("going to persist data for batch :: {}", guid);
    PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoaded(penWebBlobEntity, batchFile); // batch file can be processed further and persisted.
    for (var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      var penRequestBatchStudentEntity = mapper.toPenRequestBatchStudentEntity(student, entity);
      penRequestBatchStudentEntity.setRecordNumber(counter++);
      entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
    }
    getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
    if (entity.getPenRequestBatchID() != null) { // this could happen when the same submission number is picked up again, system should not process the same submission.
      // the entity was saved in propagation new context , so system needs to get it again from DB to have an attached entity bound to the current thread.
      final Optional<PenRequestBatchEntity> penRequestBatchEntityOptional = getPenRequestBatchFileService().findEntity(entity.getPenRequestBatchID());
      if (penRequestBatchEntityOptional.isPresent()) {
        var noRepeatsEntity = getPenRequestBatchFileService().filterDuplicatesAndRepeatRequests(guid, penRequestBatchEntityOptional.get());
        return noRepeatsEntity.stream()
            .map(studentSagaDataMapper::toPenReqBatchStudentSagaData)
            .peek(element -> {
              element.setMincode(entity.getMincode());
              element.setPenRequestBatchID(entity.getPenRequestBatchID());
            }).collect(Collectors.toSet());
      } else {
        log.info("system knows it wont happen");
      }
    }
    return new HashSet<>();
  }


  /**
   * Populate batch file.
   *
   * @param guid      the guid
   * @param ds        the ds
   * @param batchFile the batch file
   * @throws FileUnProcessableException the file un processable exception
   */
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

  /**
   * Gets student detail record from file.
   *
   * @param ds    the ds
   * @param guid  the guid
   * @param index the index
   * @return the student detail record from file
   * @throws FileUnProcessableException the file un processable exception
   */
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

  /**
   * Sets header or trailer.
   *
   * @param ds        the ds
   * @param batchFile the batch file
   * @param guid      the guid
   * @throws FileUnProcessableException the file un processable exception
   */
  private void setHeaderOrTrailer(final DataSet ds, final BatchFile batchFile, String guid) throws FileUnProcessableException {
    if (ds.isRecordID(HEADER.getName())) {
      var mincode = ds.getString(MIN_CODE.getName());
      validateMincode(guid, mincode);
      batchFile.setBatchFileHeader(BatchFileHeader.builder()
          .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
          .contactName(ds.getString(CONTACT_NAME.getName()))
          .emailID(ds.getString(EMAIL.getName()))
          .faxNumber(ds.getString(FAX_NUMBER.getName()))
          .mincode(mincode)
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

  /**
   * this method validates the min code. it calls out to school api and if there is no data in school api for the mincode then it fails
   *
   * @param guid    the guid
   * @param mincode the min code
   * @throws FileUnProcessableException the file un processable exception
   */
  private void validateMincode(String guid, String mincode) throws FileUnProcessableException {
    if (!StringUtils.isNumeric(mincode) || mincode.length() != 8) {
      throw new FileUnProcessableException(INVALID_MINCODE_HEADER, guid);
    }

    try {
      Optional<School> school = restUtils.getSchoolByMincode(mincode);

      if(school.isEmpty()) {
        throw new FileUnProcessableException(INVALID_MINCODE_HEADER, guid);
      }

      String openedDate = school.get().getOpenedDate();
      String closedDate = school.get().getClosedDate();

      if(openedDate == null || LocalDate.parse(openedDate,dateTimeFormatter).isAfter(LocalDate.now()) || (closedDate != null && LocalDate.parse(closedDate, dateTimeFormatter).isBefore(LocalDate.now()))){
        throw new FileUnProcessableException(INVALID_MINCODE_SCHOOL_CLOSED, guid);
      }
    } catch (DateTimeParseException e) {
      log.error("Date time parse exception trying to parse School's open or closed date: {}", guid, e);
      throw new FileUnProcessableException(INVALID_MINCODE_SCHOOL_CLOSED, guid);
    }
  }
}
