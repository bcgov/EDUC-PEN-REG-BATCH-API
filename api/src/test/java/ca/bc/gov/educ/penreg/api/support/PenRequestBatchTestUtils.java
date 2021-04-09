package ca.bc.gov.educ.penreg.api.support;

import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.penreg.api.batch.mappers.PenRequestBatchFileMapper;
import ca.bc.gov.educ.penreg.api.batch.processor.PenRegBatchProcessor;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaEnum;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.*;
import ca.bc.gov.educ.penreg.api.repository.*;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Component
@Profile("test")
public class PenRequestBatchTestUtils {
  @Autowired
  PenCoordinatorRepository coordinatorRepository;
  @Autowired
  PenRequestBatchStudentInfoRequestMacroRepository penRequestBatchStudentInfoRequestMacroRepository;
  @Autowired
  SagaRepository sagaRepository;

  @Autowired
  SagaEventRepository sagaEventRepository;
  /**
   * The Min.
   */
  static final int MIN = 1000000;
  /**
   * The Max.
   */
  static final int MAX = 9999999;
  @Autowired
  private PenRequestBatchRepository repository;


  @Autowired
  private PenRequestBatchHistoryRepository penRequestBatchHistoryRepository;

  /**
   * The Student repository.
   */
  @Autowired
  private PenRequestBatchStudentRepository studentRepository;

  /**
   * The Pen web blob repository.
   */
  @Autowired
  private PenWebBlobRepository penWebBlobRepository;

  @Autowired
  private PenRegBatchProcessor penRegBatchProcessor;
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  public static final String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";
  /**
   * The constant mapper.
   */
  private static final PenRequestBatchMapper mapper = PenRequestBatchMapper.mapper;

  /**
   * Populate audit columns pen request batch entity.
   *
   * @param model the model
   * @return the pen request batch entity
   */
  public static PenRequestBatchEntity populateAuditColumns(final PenRequestBatchEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
    model.setPenRequestBatchProcessTypeCode(PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode());
    return model;
  }

  /**
   * Populate audit columns pen request batch entity.
   *
   * @param penRequestBatchEntity the model
   * @return the pen request batch entity
   */
  public static PenRequestBatchEntity populateAuditColumnsAndHistory(final PenRequestBatchEntity penRequestBatchEntity) {
    populateAuditColumns(penRequestBatchEntity);
    final PenRequestBatchHistoryEntity penRequestBatchHistory = PenRequestBatchHistoryMapper.mapper.toModelFromBatch(penRequestBatchEntity, PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
    penRequestBatchEntity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
    return penRequestBatchEntity;
  }

  /**
   * Populate audit columns pen request batch student entity.
   *
   * @param model the model
   * @return the pen request batch student entity
   */
  public static PenRequestBatchStudentEntity populateAuditColumns(final PenRequestBatchStudentEntity model) {
    if (model.getCreateUser() == null) {
      model.setCreateUser(PEN_REQUEST_BATCH_API);
    }
    if (model.getUpdateUser() == null) {
      model.setUpdateUser(PEN_REQUEST_BATCH_API);
    }
    model.setCreateDate(LocalDateTime.now());
    model.setUpdateDate(LocalDateTime.now());
    return model;
  }

  /**
   * Create batch students list.
   *
   * @param penRequestBatchRepository the PenRequestBatchRepository
   * @param batchFileName the json file of batch data
   * @param batchStudentFileName the json file of batch student data
   * @param total the total
   * @param batchConsumer the function to make changes on the batch entity
   * @return the list
   */
  public static List<PenRequestBatchEntity> createBatchStudents(final PenRequestBatchRepository penRequestBatchRepository, final String batchFileName,
                                                                final String batchStudentFileName, final Integer total, final Consumer<PenRequestBatchEntity> batchConsumer) throws java.io.IOException {
    final File file = new File(
        Objects.requireNonNull(PenRequestBatchTestUtils.class.getClassLoader().getResource(batchFileName)).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(toList()).stream().map(PenRequestBatchTestUtils::populateAuditColumns).collect(toList());

    for (int i = 0; i < total && i < models.size(); i++) {
      final File student = new File(
          Objects.requireNonNull(PenRequestBatchTestUtils.class.getClassLoader().getResource(batchStudentFileName)).getFile()
      );
      final List<PenRequestBatchStudentEntity> studentEntities = new ObjectMapper().readValue(student, new TypeReference<>() {
      });
      final PenRequestBatchEntity batch = models.get(i);
      final var students = studentEntities.stream().map(PenRequestBatchTestUtils::populateAuditColumns).peek(el -> el.setPenRequestBatchEntity(batch)).collect(Collectors.toSet());

      if (batchConsumer != null) {
        batchConsumer.accept(batch);
      }
      batch.setPenRequestBatchStudentEntities(students);
    }

    penRequestBatchRepository.saveAll(models);

    return models;
  }

  public static List<Student> createStudents(final PenRequestBatchEntity penRequestBatchEntity) {

    List<Student> students = new ArrayList<>();
    for(PenRequestBatchStudentEntity penRequestBatchStudentEntity : penRequestBatchEntity.getPenRequestBatchStudentEntities()) {
      students.add(Student.builder()
        .mincode(penRequestBatchEntity.getMincode())
        .genderCode(penRequestBatchStudentEntity.getGenderCode())
        .gradeCode(penRequestBatchStudentEntity.getGradeCode())
        .legalFirstName(penRequestBatchStudentEntity.getLegalFirstName())
        .legalLastName(penRequestBatchStudentEntity.getLegalLastName())
        .legalMiddleNames(penRequestBatchStudentEntity.getLegalMiddleNames())
        .dob(penRequestBatchStudentEntity.getDob())
        .localID(penRequestBatchStudentEntity.getLocalID())
        .pen(penRequestBatchStudentEntity.getAssignedPEN())
        .studentID(UUID.randomUUID().toString())
        .build());
      if(penRequestBatchStudentEntity.getStudentID()!=null){
        students.get(students.size()-1).setStudentID(penRequestBatchStudentEntity.getStudentID().toString());
      }
    }
    return students;
  }

  public static List<PenRequestBatchEntity> createBatchStudents(final PenRequestBatchRepository penRequestBatchRepository, final String batchFileName,
                                                                final String batchStudentFileName, final Integer total) throws java.io.IOException {
    return createBatchStudents(penRequestBatchRepository, batchFileName, batchStudentFileName, total, null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createBatchStudentsInSingleTransaction(final PenRequestBatchRepository penRequestBatchRepository, final String batchFileName,
                                                         final String batchStudentFileName, final Integer total, final Consumer<PenRequestBatchEntity> batchConsumer) throws java.io.IOException {
    createBatchStudents(penRequestBatchRepository, batchFileName, batchStudentFileName, total, batchConsumer);
  }

  public static List<Saga> createSagaRecords(final SagaRepository sagaRepository, final List<PenRequestBatchEntity> batches) {
    final var studentSagaRecords = batches.stream().flatMap(batch ->
        batch.getPenRequestBatchStudentEntities().stream().map(student ->
            Saga
                .builder()
                .payload("")
                .penRequestBatchStudentID(student.getPenRequestBatchStudentID())
                .penRequestBatchID(batch.getPenRequestBatchID())
                .sagaName(SagaEnum.PEN_REQUEST_BATCH_STUDENT_PROCESSING_SAGA.toString())
                .status(COMPLETED.toString())
                .sagaState(MARK_SAGA_COMPLETE.toString())
                .createDate(LocalDateTime.now())
                .createUser(PEN_REQUEST_BATCH_API)
                .updateUser(PEN_REQUEST_BATCH_API)
                .updateDate(LocalDateTime.now())
                .build()
        )).collect(toList());
    sagaRepository.saveAll(studentSagaRecords);
    return studentSagaRecords;
  }

  /**
   * make sure the file is a valid file, free from formatting errors.
   * it will return the submission number for future use.
   *
   * @param blobFileName the name of the file from resources folder.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String createBatchStudentsFromFile(final String blobFileName, final String studentStatusCode) throws java.io.IOException,
      FileUnProcessableException {
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(blobFileName)).getFile());
      final byte[] bFile = Files.readAllBytes(file.toPath());
      final Optional<Reader> batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(bFile)));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      final var randomNum = (new Random().nextLong() * (MAX - MIN + 1) + MIN);

      final BatchFile batchFile = new BatchFile();
      this.penRegBatchProcessor.populateBatchFile(UUID.randomUUID().toString(), ds, batchFile);

      assertThat(batchFile.getBatchFileHeader()).isNotNull();
      assertThat(batchFile.getBatchFileHeader().getMincode()).isNotNull();
      final String submissionNumber = ("T" + randomNum).substring(0, 8);
      final var tsw =
          PENWebBlobEntity.builder().penWebBlobId(1L).mincode(batchFile.getBatchFileHeader().getMincode()).sourceApplication("TSW").tswAccount((randomNum + "").substring(0, 8)).fileName(blobFileName).fileType("PEN").fileContents(bFile).insertDateTime(LocalDateTime.now()).submissionNumber(submissionNumber).build();
      final PenRequestBatchEntity entity =
          PenRequestBatchFileMapper.mapper.toPenReqBatchEntityLoaded(tsw, batchFile); // batch file can be processed
      // further and persisted.
      var counter = 1;
      for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
        final var penRequestBatchStudentEntity = PenRequestBatchFileMapper.mapper.toPenRequestBatchStudentEntity(student, entity);
        penRequestBatchStudentEntity.setRecordNumber(counter++);
        penRequestBatchStudentEntity.setPenRequestBatchStudentStatusCode(studentStatusCode);
        entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
      }
      final PenRequestBatchHistoryEntity penRequestBatchHistory =
          PenRequestBatchHistoryMapper.mapper.toModelFromBatch(entity,
              PenRequestBatchEventCodes.STATUS_CHANGED.getCode());
      entity.getPenRequestBatchHistoryEntities().add(penRequestBatchHistory);
      this.repository.save(entity);
      return submissionNumber;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateBatchInNewTransaction(final PenRequestBatchEntity penRequestBatchEntity) {
    this.repository.save(penRequestBatchEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.coordinatorRepository.deleteAll();
    this.penRequestBatchStudentInfoRequestMacroRepository.deleteAll();
    this.sagaEventRepository.deleteAll();
    this.sagaRepository.deleteAll();
    this.studentRepository.deleteAll();
    this.penRequestBatchHistoryRepository.deleteAll();
    this.penWebBlobRepository.deleteAll();
    this.repository.deleteAll();

  }
}
