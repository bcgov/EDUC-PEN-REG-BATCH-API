package ca.bc.gov.educ.penreg.api.support;

import ca.bc.gov.educ.penreg.api.constants.SagaEnum;
import ca.bc.gov.educ.penreg.api.mappers.v1.PenRequestBatchMapper;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.model.PenRequestBatchStudentEntity;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.penreg.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.penreg.api.constants.SagaStatusEnum.COMPLETED;
import static java.util.stream.Collectors.toList;

public class PenRequestBatchUtils {
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
  public static PenRequestBatchEntity populateAuditColumns(PenRequestBatchEntity model) {
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
   * Populate audit columns pen request batch student entity.
   *
   * @param model the model
   * @return the pen request batch student entity
   */
  public static PenRequestBatchStudentEntity populateAuditColumns(PenRequestBatchStudentEntity model) {
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
   * @throws IOException the io exception
   */
  public static List<PenRequestBatchEntity> createBatchStudents(PenRequestBatchRepository penRequestBatchRepository, String batchFileName,
                                                                String batchStudentFileName, Integer total, Consumer<PenRequestBatchEntity> batchConsumer) throws java.io.IOException {
    final File file = new File(
      Objects.requireNonNull(PenRequestBatchUtils.class.getClassLoader().getResource(batchFileName)).getFile()
    );
    List<PenRequestBatch> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(mapper::toModel).collect(toList()).stream().map(PenRequestBatchUtils::populateAuditColumns).collect(toList());

    for(int i = 0; i < total && i < models.size(); i++) {
      final File student = new File(
        Objects.requireNonNull(PenRequestBatchUtils.class.getClassLoader().getResource(batchStudentFileName)).getFile()
      );
      List<PenRequestBatchStudentEntity> studentEntities = new ObjectMapper().readValue(student, new TypeReference<>() {
      });
      final PenRequestBatchEntity batch = models.get(i);
      var students = studentEntities.stream().map(PenRequestBatchUtils::populateAuditColumns).peek(el -> el.setPenRequestBatchEntity(batch)).collect(Collectors.toSet());

      if(batchConsumer != null) {
        batchConsumer.accept(batch);
      }
      batch.setPenRequestBatchStudentEntities(students);
    }

    penRequestBatchRepository.saveAll(models);

    return models;
  }

  public static List<PenRequestBatchEntity> createBatchStudents(PenRequestBatchRepository penRequestBatchRepository, String batchFileName,
                                                                String batchStudentFileName, Integer total) throws java.io.IOException {
    return createBatchStudents(penRequestBatchRepository, batchFileName, batchStudentFileName, total, null);
  }

  public static List<Saga> createSagaRecords(SagaRepository sagaRepository, List<PenRequestBatchEntity> batches) {
    var studentSagaRecords = batches.stream().flatMap(batch ->
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
}
