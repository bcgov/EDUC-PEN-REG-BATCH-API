package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.constants.PenRequestBatchStudentStatusCodes;
import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;
import ca.bc.gov.educ.penreg.api.model.v1.PenCoordinator;
import ca.bc.gov.educ.penreg.api.model.v1.PenRequestBatchEntity;
import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository;
import ca.bc.gov.educ.penreg.api.repository.PenWebBlobRepository;
import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Response file generator service.
 *
 * @author JISUNG
 */
@Service
@Slf4j
public class ResponseFileGeneratorService {

  public static final String SOURCE_APPLICATION = "PENWEB";
  private static final int PAR_FILE_PAGE_LINES = 52;
  private static final byte[] EMPTY_TXT_FILE_CONTENT = "No errors have been identified in this request".getBytes();
  /**
   * the pen coordinator service
   */
  @Getter(PRIVATE)
  private final PenCoordinatorService penCoordinatorService;

  /**
   * the pen request batch student repository
   */
  @Getter(PRIVATE)
  private final PenRequestBatchStudentRepository penRequestBatchStudentRepository;

  /**
   * The Pen web blob repository.
   */
  @Getter(PRIVATE)
  private final PenWebBlobRepository penWebBlobRepository;

  /**
   * The Rest utils.
   */
  @Getter(PRIVATE)
  private final RestUtils restUtils;

  /**
   * the Thymeleaf template engine
   */
  @Getter(PRIVATE)
  private SpringTemplateEngine templateEngine;

  @Autowired
  public ResponseFileGeneratorService(final PenCoordinatorService penCoordinatorService, final PenWebBlobRepository penWebBlobRepository, final PenRequestBatchStudentRepository penRequestBatchStudentRepository, final RestUtils restUtils, final SpringTemplateEngine templateEngine) {
    this.penCoordinatorService = penCoordinatorService;
    this.penWebBlobRepository = penWebBlobRepository;
    this.penRequestBatchStudentRepository = penRequestBatchStudentRepository;
    this.restUtils = restUtils;
    this.templateEngine = templateEngine;
  }
  /**
   * Create an IDS file for a pen request batch entity
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen web blob entity
   */
  public PENWebBlobEntity getIDSBlob(final PenRequestBatchEntity penRequestBatchEntity, final List<PenRequestBatchStudent> penRequestBatchStudentEntities, final List<Student> students) {

    Map<String, Student> studentsMap = students.stream().collect(Collectors.toMap(Student::getStudentID, student -> student));
    final List<PenRequestBatchStudent> filteredStudents = penRequestBatchStudentEntities.stream().filter(x ->
            (x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.SYS_NEW_PEN.getCode()) ||
            x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.USR_NEW_PEN.getCode()) ||
            x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.SYS_MATCHED.getCode()) ||
            x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.USR_MATCHED.getCode())) &&
            x.getLocalID() != null).collect(Collectors.toList());

    byte[] bFile = null;
    if (!filteredStudents.isEmpty()) {
      final StringBuilder idsFile = new StringBuilder();

      for (final PenRequestBatchStudent entity : filteredStudents) {
        final var student = studentsMap.get(entity.getStudentID());
        if(student != null) {
          idsFile.append("E03")
          .append(student.getMincode())
          .append(String.format("%12s", student.getLocalID()).replace(' ', '0'))
          .append(student.getPen()).append(" ")
          .append(student.getLegalLastName())
          .append("\n");
        } else {
          log.error("StudentId was not found. This should not have happened.");
        }
      }
      bFile = idsFile.toString().getBytes();
    } else {
      bFile = "No NEW PENS have been assigned by this PEN request".getBytes();
    }
    return PENWebBlobEntity.builder()
            .mincode(penRequestBatchEntity.getMincode())
            .sourceApplication(SOURCE_APPLICATION)
            .fileName(penRequestBatchEntity.getMincode() + ".IDS")
            .fileType("IDS")
            .fileContents(bFile)
            .insertDateTime(LocalDateTime.now())
            .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
            .build();
  }

  /**
   * Create an TXT file for a pen request batch entity
   *
   * @param penRequestBatchEntity the pen request batch entity
   * @return the pen web blob entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public PENWebBlobEntity getTxtBlob(final PenRequestBatchEntity penRequestBatchEntity, final List<PenRequestBatchStudent> penRequestBatchStudentEntities) {

    final List<PenRequestBatchStudent> filteredStudents = penRequestBatchStudentEntities.stream().filter(x ->
            (x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode()) ||
            x.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.INFOREQ.getCode())) &&
            x.getLocalID() != null).collect(Collectors.toList());

    byte[] bFile = null;

    if (!filteredStudents.isEmpty()) {
      // retrieve the original prb file from school
      List<PENWebBlobEntity> penWebBlobs = this.getPenWebBlobRepository().findAllBySubmissionNumberAndFileType(penRequestBatchEntity.getSubmissionNumber(), "PEN");
      PENWebBlobEntity penWebBlob = penWebBlobs.isEmpty() ? null : penWebBlobs.get(0);

      Pair<String, Map<String, String>> pair = parseOriginalPenFile(penWebBlob != null ? penWebBlob.getFileContents() : null);
      String applicationCode = pair.getFirst();
      Map<String, String> applicationKeyMap = pair.getSecond();

      final StringBuilder txtFile = new StringBuilder();
      // FFI header
      txtFile.append(createHeader(penRequestBatchEntity, applicationCode));

      // SRM details records
      for (final PenRequestBatchStudent entity : penRequestBatchStudentEntities) {
        if (entity.getPenRequestBatchStudentStatusCode().equals(PenRequestBatchStudentStatusCodes.ERROR.getCode())) {
          txtFile.append(createBody(entity, applicationKeyMap));
        }
      }
      // BTR footer
      txtFile.append(createFooter(penRequestBatchEntity));
      bFile = txtFile.toString().getBytes();
    } else {
      bFile = EMPTY_TXT_FILE_CONTENT;
    }
    return PENWebBlobEntity.builder()
            .mincode(penRequestBatchEntity.getMincode())
            .sourceApplication(SOURCE_APPLICATION)
            .fileName(penRequestBatchEntity.getMincode() + ".TXT")
            .fileType("TXT")
            .fileContents(bFile)
            .insertDateTime(LocalDateTime.now())
            .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
            .build();
  }

  public PENWebBlobEntity getPDFBlob(String pdfReport, PenRequestBatchEntity penRequestBatchEntity) {
    return PENWebBlobEntity.builder()
            .mincode(penRequestBatchEntity.getMincode())
            .sourceApplication(SOURCE_APPLICATION)
            .fileName(penRequestBatchEntity.getMincode() + ".PDF")
            .fileType("PDF")
            .fileContents(Base64.getDecoder().decode(pdfReport.getBytes(StandardCharsets.UTF_8)))
            .insertDateTime(LocalDateTime.now())
            .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
            .build();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void savePDFReport(final String pdfReport, PenRequestBatchEntity penRequestBatchEntity) {
    this.getPenWebBlobRepository().save(this.getPDFBlob(pdfReport, penRequestBatchEntity));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveReports(final String pdfReport, PenRequestBatchEntity penRequestBatchEntity, List<PenRequestBatchStudent> penRequestBatchStudents,
                          List<Student> students, PenRequestBatchReportData reportData) {
    List<PENWebBlobEntity> reports = new ArrayList<>(Arrays.asList(
      this.getPDFBlob(pdfReport, penRequestBatchEntity),
      this.getIDSBlob(penRequestBatchEntity, penRequestBatchStudents, students)));
    if (penRequestBatchEntity.getSchoolGroupCode().equals(SchoolGroupCodes.PSI.getCode())) {
      reports.add(this.getPARBlob(reportData, penRequestBatchEntity));
      reports.add(this.getTxtBlob(penRequestBatchEntity, penRequestBatchStudents));
    }
    this.getPenWebBlobRepository().saveAll(reports);
  }

  /**
   * Create an PAR file for a pen request batch entity
   *
   * @param reportData the pen request batch report data
   * @return the pen web blob entity
   */
  public PENWebBlobEntity getPARBlob(final PenRequestBatchReportData reportData, final PenRequestBatchEntity penRequestBatchEntity) {
    final Context ctx = new Context();
    ctx.setVariable("d", reportData);
    String content;

    content = this.templateEngine.process("PEN_REG_BATCH_RESPONSE_REPORT_PAR.txt", ctx);
    var lines = content.lines().collect(Collectors.toList());

    var pages = lines.size() / PAR_FILE_PAGE_LINES + 1;
    for (int i = 0; i < pages; i++) {
      ctx.setVariable("pageNumber", String.valueOf(i + 1));
      var header = this.templateEngine.process("PEN_REG_BATCH_RESPONSE_REPORT_PAR_HEADER.txt", ctx);
      lines.add((PAR_FILE_PAGE_LINES + 1) * i, (i > 0 ? "\n" : "") + header);
    }

    content = String.join("\n", lines);

    return PENWebBlobEntity.builder()
      .mincode(penRequestBatchEntity.getMincode())
      .sourceApplication(SOURCE_APPLICATION)
      .fileName(penRequestBatchEntity.getMincode() + ".PAR")
      .fileType("PAR")
      .fileContents(content.getBytes())
      .insertDateTime(LocalDateTime.now())
      .submissionNumber(penRequestBatchEntity.getSubmissionNumber())
      .build();
  }

  private String createHeader(final PenRequestBatchEntity penRequestBatchEntity, String applicationCode) {
    final StringBuilder header = new StringBuilder();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    // retrieved from PEN_COORDINATOR table
    Optional<PenCoordinator> penCoordinator = this.getPenCoordinatorService().getPenCoordinatorByMinCode(penRequestBatchEntity.getMincode());

    header.append("FFI")
            .append(String.format("%-8.8s", print(penRequestBatchEntity.getMincode())))
            .append(String.format("%-40.40s", print(penRequestBatchEntity.getSchoolName())))
            .append(String.format("%-8.8s", dateFormat.format(new Date())))
            .append(String.format("%-100.100s", print(penCoordinator.isPresent()? penCoordinator.get().getPenCoordinatorEmail() : "")))
            .append(String.format("%-10.10s", print(penCoordinator.map(coordinator -> coordinator.getPenCoordinatorFax().replaceAll("[^0-9]+", "")).orElse(""))))
            .append(String.format("%-40.40s", print(penCoordinator.isPresent()? penCoordinator.get().getPenCoordinatorName() : "")))
            .append("  ")
            .append(String.format("%-4.4s", print(applicationCode)))
            .append("\n");

    return header.toString();
  }

  private String createFooter(final PenRequestBatchEntity penRequestBatchEntity) {
    return "BTR" +
            String.format("%04d", print(penRequestBatchEntity.getSourceStudentCount())) +
            String.format("%-100.100s", print(penRequestBatchEntity.getSisVendorName())) +
            String.format("%-100.100s", print(penRequestBatchEntity.getSisProductName())) +
            String.format("%-15.15s", print(penRequestBatchEntity.getSisProductID())) +
            "\n";
  }

  private String createBody(final PenRequestBatchStudent penRequestBatchStudentEntity, Map<String,String> applicationKeyMap) {
    final StringBuilder body = new StringBuilder();

    String localID = StringUtils.leftPad(penRequestBatchStudentEntity.getLocalID(), 12, "0");
    String applicationKey = applicationKeyMap.get(localID);

    body.append("SRM")
            .append(String.format("%-12.12s", print(localID)))
            .append(String.format("%-10.10s", print(penRequestBatchStudentEntity.getSubmittedPen())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalLastName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalFirstName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getLegalMiddleNames())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualLastName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualFirstName())))
            .append(String.format("%-25.25s", print(penRequestBatchStudentEntity.getUsualMiddleNames())))
            .append(String.format("%-8.8s", print(penRequestBatchStudentEntity.getDob())))
            .append(String.format("%1.1s", print(penRequestBatchStudentEntity.getGenderCode())))
            .append(StringUtils.leftPad("", 16, " "))
            .append(String.format("%-2.2s", print(penRequestBatchStudentEntity.getGradeCode())))
            .append(StringUtils.leftPad("", 26, " "))
            .append(String.format("%-7.7s", print(penRequestBatchStudentEntity.getPostalCode())))
            .append(String.format("%-20.20s", print(applicationKey)))
            .append("\n");

    return body.toString();
  }

  private Pair<String, Map<String, String>> parseOriginalPenFile(byte[] fileContents) {
    String applicationCode = "";
    Map<String, String> applicationKeyMap = new HashMap<>();

    if (fileContents == null || fileContents.length == 0) {
      return Pair.of(applicationCode, applicationKeyMap);
    }

    try {
      InputStreamReader inStreamReader = new InputStreamReader(new ByteArrayInputStream(fileContents));
      BufferedReader reader = new BufferedReader(inStreamReader);

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("FFI")) {
          applicationCode = line.substring(211,215).trim();
        } else if (line.startsWith("SRM")) {
          String currentLocalID = line.substring(3, 15);
          applicationKeyMap.put(currentLocalID, line.substring(235, 255).trim());
        }
      }
    } catch (Exception e) {
      log.error("An unexpected error occurred while attempting to parseOriginalPenFile :: ", e);
    }
    return Pair.of(applicationCode, applicationKeyMap);
  }

  private String print(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }

  private Long print(Long value) {
    if (value == null) {
      return 0L;
    }
    return value;
  }
}
