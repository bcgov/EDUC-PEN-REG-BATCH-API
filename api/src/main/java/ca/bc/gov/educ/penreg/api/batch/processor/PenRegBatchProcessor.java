package ca.bc.gov.educ.penreg.api.batch.processor;

import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.DUPLICATE_BATCH_FILE_PSI;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.HELD_BACK_FOR_SFAS;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.HELD_BACK_FOR_SIZE;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.INVALID_MINCODE_HEADER;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.INVALID_MINCODE_SCHOOL_CLOSED;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.INVALID_TRAILER;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.INVALID_TRAILER_STUDENT_COUNT;
import static ca.bc.gov.educ.penreg.api.batch.exception.FileError.INVALID_TRANSACTION_CODE_STUDENT_DETAILS;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.BIRTH_DATE;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.ENROLLED_GRADE_CODE;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.GENDER;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.HEADER;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.LEGAL_GIVEN_NAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.LEGAL_MIDDLE_NAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.LEGAL_SURNAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.LOCAL_STUDENT_ID;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.PEN;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.POSTAL_CODE;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.PRODUCT_ID;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.PRODUCT_NAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.STUDENT_COUNT;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.TRAILER;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.TRANSACTION_CODE;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.UNUSED;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.UNUSED_SECOND;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.USUAL_GIVEN_NAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.USUAL_MIDDLE_NAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.USUAL_SURNAME;
import static ca.bc.gov.educ.penreg.api.constants.BatchFileConstants.VENDOR_NAME;
import static lombok.AccessLevel.PRIVATE;

import ca.bc.gov.educ.penreg.api.batch.exception.FileError;
import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper;
import ca.bc.gov.educ.penreg.api.batch.mappers.StringMapper;
import ca.bc.gov.educ.penreg.api.batch.service.DuplicateFileCheckService;
import ca.bc.gov.educ.penreg.api.batch.service.PenRequestBatchFileService;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileHeader;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileTrailer;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.penreg.api.batch.validator.PenRequestBatchFileValidator;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.service.NotificationService;
import ca.bc.gov.educ.penreg.api.service.StudentRegistrationContactService;
import ca.bc.gov.educ.penreg.api.struct.School;
import com.google.common.base.Stopwatch;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
   * The Pen reg batch student records processor.
   */
  @Getter(PRIVATE)
  private final PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor;
  /**
   * The constant TRANSACTION_CODE_STUDENT_DETAILS_RECORD.
   */
  public static final String TRANSACTION_CODE_STUDENT_DETAILS_RECORD = "SRM";

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
  @Getter(PRIVATE)
  private final RestUtils restUtils;

  /**
   * The Notification service.
   */
  private final NotificationService notificationService;
  /**
   * The Pen coordinator service.
   */
  private final StudentRegistrationContactService penCoordinatorService;

  /**
   * The Duplicate file check service map.
   */
  private final Map<SchoolGroupCodes, DuplicateFileCheckService> duplicateFileCheckServiceMap;
  /**
   * The Pen request batch file validator.
   */
  private final PenRequestBatchFileValidator penRequestBatchFileValidator;

  /**
   * Instantiates a new Pen reg batch processor.
   *
   * @param penRegBatchStudentRecordsProcessor the pen reg batch student records processor
   * @param penRequestBatchFileService         the pen request batch file service
   * @param applicationProperties              the application properties
   * @param notificationService                the notification service
   * @param penCoordinatorService              the pen coordinator service
   * @param duplicateFileCheckServiceList      the duplicate file check service list
   * @param penRequestBatchFileValidator       the pen request batch file validator
   */
  @Autowired
  public PenRegBatchProcessor(final PenRegBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor, final PenRequestBatchFileService penRequestBatchFileService, final ApplicationProperties applicationProperties, final NotificationService notificationService, final StudentRegistrationContactService penCoordinatorService, final List<DuplicateFileCheckService> duplicateFileCheckServiceList, final PenRequestBatchFileValidator penRequestBatchFileValidator, final RestUtils restUtils) {
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
    this.penRequestBatchFileService = penRequestBatchFileService;
    this.applicationProperties = applicationProperties;
    this.notificationService = notificationService;
    this.penCoordinatorService = penCoordinatorService;
    this.duplicateFileCheckServiceMap = duplicateFileCheckServiceList.stream().collect(Collectors.toMap(DuplicateFileCheckService::getSchoolGroupCode, Function.identity()));
    this.penRequestBatchFileValidator = penRequestBatchFileValidator;
    this.restUtils = restUtils;
  }

  /**
   * Process pen reg batch file from tsw.
   * 1. <p>The data comes from TSW table so if the the data from the TSW table cant be read error is logged and email is sent.</p>
   * 2. <p>If The data is successfully retrieved from TSW table and file header cant be parsed, system will create only the header record and persist it.
   *
   * @param penWebBlob the pen web blob entity
   */
  @Transactional
  @Async("penWebBlobExtractor")
  public void processPenRegBatchFileFromPenWebBlob(@NonNull final PENWebBlobEntity penWebBlob) {
    val penWebBlobEntity = this.penRequestBatchFileService.getPenWebBlob(penWebBlob.getPenWebBlobId()).orElseThrow(); // do a get to associate the object with current thread for lazy loading.
    val stopwatch = Stopwatch.createStarted();
    final var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing row from Pen Web Blobs with submission Number :: {} and guid :: {}", penWebBlobEntity.getSubmissionNumber(), guid);
    val batchFile = new BatchFile();
    Optional<Reader> batchFileReaderOptional = Optional.empty();
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      var byteArrayOutputStream = new ByteArrayInputStream(penWebBlobEntity.getFileContents());
      var encoding = UniversalDetector.detectCharset(byteArrayOutputStream);
      byteArrayOutputStream.reset();
      if(!StringUtils.isEmpty(encoding) && !encoding.equals("UTF-8")){
        encoding = "windows-1252";
      }

      if(StringUtils.isEmpty(encoding)){
        batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
      }else{
        batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream, Charset.forName(encoding).newDecoder()));
      }
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      this.penRequestBatchFileValidator.validateFileForFormatAndLength(guid, ds);
      this.penRequestBatchFileValidator.validateMincode(guid, penWebBlobEntity.getMincode());
      this.populateBatchFile(guid, ds, batchFile);
      this.penRequestBatchFileValidator.validateStudentCountForMismatchAndSize(guid, batchFile, penWebBlobEntity.getMincode());
      this.checkForDuplicateFile(penWebBlobEntity, guid); // if all other validations passed check if it is a duplicate file from PSI.
      this.processLoadedRecordsInBatchFile(guid, batchFile, penWebBlobEntity);
    } catch (final FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      this.processFileUnProcessableException(guid, penWebBlobEntity, fileUnProcessableException, batchFile);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
    } finally {
      batchFileReaderOptional.ifPresent(this::closeBatchFileReader);
      stopwatch.stop();
      log.info("Time taken for batch processed is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  /**
   * Close batch file reader.
   *
   * @param reader the reader
   */
  private void closeBatchFileReader(final Reader reader) {
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (final IOException e) {
      log.warn("Error closing the batch file :: ", e);
    }
  }


  /**
   * Check for duplicate file.
   *
   * @param penWebBlobEntity the pen web blob entity
   * @param guid             the guid
   * @throws FileUnProcessableException the file un processable exception
   */
  private void checkForDuplicateFile(final PENWebBlobEntity penWebBlobEntity, final String guid) throws FileUnProcessableException {
    if (StringUtils.startsWith(penWebBlobEntity.getMincode(), "102")
      && this.duplicateFileCheckServiceMap.get(SchoolGroupCodes.PSI).isBatchFileDuplicate(penWebBlobEntity)) {
      throw new FileUnProcessableException(DUPLICATE_BATCH_FILE_PSI, guid, PenRequestBatchStatusCodes.DUPLICATE);
    }
  }

  /**
   * Persist data with exception.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   * @param fileUnProcessableException the file un processable exception
   * @param batchFile                  the batch file
   */
  private void processFileUnProcessableException(@NonNull final String guid, @NonNull final PENWebBlobEntity penWebBlobEntity, @NonNull final FileUnProcessableException fileUnProcessableException, final BatchFile batchFile) {
    val notifySchoolForFileFormatErrorsOptional = this.notifySchoolForFileFormatErrors(guid, penWebBlobEntity, fileUnProcessableException);
    final PenRequestBatchEntity entity = mapper.toPenReqBatchEntityForBusinessException(penWebBlobEntity, fileUnProcessableException.getReason(), fileUnProcessableException.getPenRequestBatchStatusCode(), batchFile, persistStudentRecords(fileUnProcessableException.getFileError())); // batch file can be processed further and persisted.
    final Optional<School> school = this.restUtils.getSchoolByMincode(penWebBlobEntity.getMincode());
    school.ifPresent(value -> entity.setSchoolName(value.getSchoolName()));
    //wait here if notification was sent, if there was any error this file will be picked up again as it wont be persisted.
    if (notifySchoolForFileFormatErrorsOptional.isPresent()) {
      final boolean isNotified = this.waitForNotificationToCompleteIfPresent(guid, notifySchoolForFileFormatErrorsOptional.get());
      if (isNotified) {
        log.info("going to persist data with FileUnProcessableException for batch :: {}", guid);
        this.getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
      } else {
        log.warn("Batch file could not be persisted as system was not able to send required notification to school, it will be picked up again by the scheduler.");
      }
    } else {
      log.info("going to persist data with FileUnProcessableException for batch :: {}", guid);
      this.getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
    }
  }
  private boolean persistStudentRecords(FileError fileError) {
    return fileError == HELD_BACK_FOR_SIZE || fileError == HELD_BACK_FOR_SFAS;
  }

  /**
   * Wait for notification to complete if present.
   *
   * @param guid                     the guid
   * @param booleanCompletableFuture the boolean completable future
   * @return the boolean
   */
  private boolean waitForNotificationToCompleteIfPresent(final String guid, final CompletableFuture<Boolean> booleanCompletableFuture) {
    try {
      final boolean isNotificationSuccessful = booleanCompletableFuture.get(2000L, TimeUnit.MILLISECONDS); // wait here for result.
      log.info("notification result for :: {} is :: {}", guid, isNotificationSuccessful);
      return isNotificationSuccessful;
    } catch (final InterruptedException e) {
      log.error("InterruptedException ", e);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | TimeoutException e) {
      log.error("ExecutionException | TimeoutException ", e);
    }
    return false;
  }

  /**
   * Notify school for file format errors optional.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   * @param fileUnProcessableException the file un processable exception
   * @return the optional
   */
  private Optional<CompletableFuture<Boolean>> notifySchoolForFileFormatErrors(final String guid, final PENWebBlobEntity penWebBlobEntity, final FileUnProcessableException fileUnProcessableException) {
    Optional<CompletableFuture<Boolean>> isSchoolNotifiedFutureOptional = Optional.empty();
    if (this.isNotificationToSchoolRequired(fileUnProcessableException)) {
      log.info("notification to school is required :: {}", guid);
      val coordinatorEmailOptional = this.penCoordinatorService.getStudentRegistrationContactEmailsByMincode(penWebBlobEntity.getMincode());
      if (coordinatorEmailOptional.isPresent()) {
        log.info("pen coordinator email found :: {}, for guid :: {}", coordinatorEmailOptional.get(), guid);
        isSchoolNotifiedFutureOptional = Optional.ofNullable(this.notificationService.notifySchoolForLoadFailed(guid, penWebBlobEntity.getFileName(), penWebBlobEntity.getSubmissionNumber(), fileUnProcessableException.getReason(), coordinatorEmailOptional.get()));
      }
    }
    return isSchoolNotifiedFutureOptional;
  }

  /**
   * Is notification to school required boolean. notify school in all other conditions than the below ones.
   *
   * @param fileUnProcessableException the file un processable exception
   * @return the boolean
   */
  private boolean isNotificationToSchoolRequired(final FileUnProcessableException fileUnProcessableException) {
    return fileUnProcessableException.getFileError() != INVALID_MINCODE_SCHOOL_CLOSED
      && fileUnProcessableException.getFileError() != INVALID_MINCODE_HEADER
      && fileUnProcessableException.getFileError() != DUPLICATE_BATCH_FILE_PSI
      && fileUnProcessableException.getFileError() != HELD_BACK_FOR_SIZE
      && fileUnProcessableException.getFileError() != HELD_BACK_FOR_SFAS;
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
   */
  private void processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final BatchFile batchFile, @NonNull final PENWebBlobEntity penWebBlobEntity) {
    var counter = 1;
    log.info("going to persist data for batch :: {}", guid);
    final PenRequestBatchEntity entity = mapper.toPenReqBatchEntityLoaded(penWebBlobEntity, batchFile); // batch file can be processed further and persisted.
    final Optional<School> school = this.restUtils.getSchoolByMincode(penWebBlobEntity.getMincode());
    school.ifPresent(value -> entity.setSchoolName(value.getSchoolName()));
    for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      final var penRequestBatchStudentEntity = mapper.toPenRequestBatchStudentEntity(student, entity);
      penRequestBatchStudentEntity.setRecordNumber(counter++);
      entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
    }
    this.getPenRequestBatchFileService().markInitialLoadComplete(entity, penWebBlobEntity);
    if (entity.getPenRequestBatchID() != null) { // this could happen when the same submission number is picked up again, system should not process the same submission.
      // the entity was saved in propagation new context , so system needs to get it again from DB to have an attached entity bound to the current thread.
      this.getPenRequestBatchFileService().filterDuplicatesAndRepeatRequests(guid, entity);
    }
  }


  /**
   * Populate batch file.
   *
   * @param guid      the guid
   * @param ds        the ds
   * @param batchFile the batch file
   * @throws FileUnProcessableException the file un processable exception
   */
  public void populateBatchFile(final String guid, final DataSet ds, final BatchFile batchFile) throws FileUnProcessableException {
    long index = 0;
    while (ds.next()) {
      if (ds.isRecordID(HEADER.getName()) || ds.isRecordID(TRAILER.getName())) {
        this.setHeaderOrTrailer(ds, batchFile);
        index++;
        continue;
      }
      batchFile.getStudentDetails().add(this.getStudentDetailRecordFromFile(ds, guid, index));
      index++;
    }

    if(batchFile.getBatchFileTrailer() == null) {
      setManualTrailer(guid, ds, batchFile);
    }
  }

  private void setManualTrailer(final String guid, final DataSet ds, final BatchFile batchFile) throws FileUnProcessableException {
    String rawTrailer = ds.getErrors().get(ds.getErrors().size()-1).getRawData();

    if(rawTrailer == null || rawTrailer.length() < 9){
      throw new FileUnProcessableException(INVALID_TRAILER, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }
    String studentCount = rawTrailer.substring(3,9).trim();
    if(!StringUtils.isNumeric(studentCount)){
      throw new FileUnProcessableException(INVALID_TRAILER_STUDENT_COUNT, guid, PenRequestBatchStatusCodes.LOAD_FAIL);
    }

    var trailer = new BatchFileTrailer();
    trailer.setStudentCount(studentCount);
    batchFile.setBatchFileTrailer(trailer);
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
  private StudentDetails getStudentDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws FileUnProcessableException {
    final var transactionCode = ds.getString(TRANSACTION_CODE.getName());
    if (!TRANSACTION_CODE_STUDENT_DETAILS_RECORD.equals(transactionCode)) {
      throw new FileUnProcessableException(INVALID_TRANSACTION_CODE_STUDENT_DETAILS, guid, PenRequestBatchStatusCodes.LOAD_FAIL, String.valueOf(index), ds.getString(LOCAL_STUDENT_ID.getName()));
    }
    return StudentDetails.builder()
      .birthDate(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(BIRTH_DATE.getName())))
      .enrolledGradeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(ENROLLED_GRADE_CODE.getName())))
      .gender(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(GENDER.getName())))
      .legalGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_GIVEN_NAME.getName())))
      .legalMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_MIDDLE_NAME.getName())))
      .legalSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_SURNAME.getName())))
      .localStudentID(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LOCAL_STUDENT_ID.getName())))
      .pen(ds.getString(PEN.getName()))
      .postalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(POSTAL_CODE.getName())))
      .transactionCode(transactionCode)
      .unused(ds.getString(UNUSED.getName()))
      .unusedSecond(ds.getString(UNUSED_SECOND.getName()))
      .usualGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_GIVEN_NAME.getName())))
      .usualMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_MIDDLE_NAME.getName())))
      .usualSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_SURNAME.getName())))
      .build();
  }

  /**
   * Sets header or trailer.
   *
   * @param ds        the ds
   * @param batchFile the batch file
   */
  private void setHeaderOrTrailer(final DataSet ds, final BatchFile batchFile) {
    if (ds.isRecordID(HEADER.getName())) {
      //Just set transactionCode because of different flavours of header
      batchFile.setBatchFileHeader(BatchFileHeader.builder()
        .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
        .build());
    } else if (ds.isRecordID(TRAILER.getName())) {
      final var transactionCode = ds.getString(TRANSACTION_CODE.getName());
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
