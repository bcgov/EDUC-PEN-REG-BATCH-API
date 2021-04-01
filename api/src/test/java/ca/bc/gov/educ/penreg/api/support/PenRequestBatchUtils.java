package ca.bc.gov.educ.penreg.api.support;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchEventCodes;
import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes;
import ca.bc.gov.educ.penreg.api.constants.SagaEnum;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchHistoryMapper;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchHistoryEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.v1.Saga;
import ca.bc.gov.educ.penreg.api.repository.*;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static java.util.stream.Collectors.toList;

@Component
@Profile("test")
public class PenRequestBatchUtils {
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
        Objects.requireNonNull(PenRequestBatchUtils.class.getClassLoader().getResource(batchFileName)).getFile()
    );
    final List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final var models = entities.stream().map(mapper::toModel).collect(toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(toList());

    for (int i = 0; i < total && i < models.size(); i++) {
      final File student = new File(
          Objects.requireNonNull(PenRequestBatchUtils.class.getClassLoader().getResource(batchStudentFileName)).getFile()
      );
      final List<PenRequestBatchStudentEntity> studentEntities = new ObjectMapper().readValue(student, new TypeReference<>() {
      });
      final PenRequestBatchEntity batch = models.get(i);
      final var students = studentEntities.stream().map(PenRequestBatchUtils::populateAuditColumns).peek(el -> el.setPenRequestBatchEntity(batch)).collect(Collectors.toSet());

      if (batchConsumer != null) {
        batchConsumer.accept(batch);
      }
      batch.setPenRequestBatchStudentEntities(students);
    }

    penRequestBatchRepository.saveAll(models);

    return models;
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.studentRepository.deleteAll();
    this.penRequestBatchHistoryRepository.deleteAll();
    this.penWebBlobRepository.deleteAll();
    this.repository.deleteAll();
  }
}
