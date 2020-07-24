package ca.bc.gov.educ.penreg.api.batch.processor;

import ca.bc.gov.educ.penreg.api.batch.struct.BatchFile;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileHeader;
import ca.bc.gov.educ.penreg.api.batch.struct.BatchFileTrailer;
import ca.bc.gov.educ.penreg.api.batch.struct.StudentDetails;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import net.sf.flatpack.Parser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

import static ca.bc.gov.educ.penreg.api.batch.constants.BatchFileConstants.*;

/**
 * The Pen reg batch processor.
 * @author OM
 */
@Component
@Slf4j
public class PenRegBatchProcessor {

  /**
   * Process pen reg batch file from tsw.
   */
  @Async
  @Transactional
  public void processPenRegBatchFileFromTSW(){
    Reader batchFileReader = null;
    try (Reader mapperReader = new FileReader(new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("mapper.xml")).getFile()))){

      batchFileReader =  new FileReader(new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sample.txt")).getFile()));
      final Parser pzParser = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader,  batchFileReader);
      final DataSet ds = pzParser.setNullEmptyStrings(true).setHandlingShortLines(true).setIgnoreExtraColumns(true).parse();
      BatchFile batchFile = new BatchFile();
      while (ds.next()) {
        if (ds.isRecordID(HEADER.getName()) || ds.isRecordID(TRAILER.getName())) {
          setHeaderOrTrailer(ds, batchFile);
          continue;
        }
        batchFile.getStudentDetails().add(getStudentDetailRecordFromFile(ds));
      }
      if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
        for (DataError error : ds.getErrors()) {
          log.warn("ERROR: " + error.getErrorDesc() + " LINE NUMBER: " + error.getLineNo());
        }
      }

    } catch (Exception e) {
      log.error("Exception while processing the file", e);
    }
    finally {
      if(batchFileReader != null){
        try {
          batchFileReader.close();
        } catch (IOException e) {
          log.warn("Error closing the batch file", e);
        }
      }
    }
  }

  private StudentDetails getStudentDetailRecordFromFile(final DataSet ds) {
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
        .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
        .unused(ds.getString(UNUSED.getName()))
        .unusedSecond(ds.getString(UNUSED_SECOND.getName()))
        .usualGivenName(ds.getString(USUAL_GIVEN_NAME.getName()))
        .usualMiddleName(ds.getString(USUAL_MIDDLE_NAME.getName()))
        .usualSurname(ds.getString(USUAL_SURNAME.getName()))
        .build();
  }

  private void setHeaderOrTrailer(final DataSet ds, final BatchFile batchFile) {
    if(ds.isRecordID(HEADER.getName())){
      batchFile.setBatchFileHeader(BatchFileHeader.builder()
          .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
          .contactName(ds.getString(CONTACT_NAME.getName()))
          .emailID(ds.getString(EMAIL.getName()))
          .faxNumber(ds.getString(FAX_NUMBER.getName()))
          .minCode(ds.getString(MIN_CODE.getName()))
          .officeNumber(ds.getString(OFFICE_NUMBER.getName()))
          .requestDate(ds.getString(REQUEST_DATE.getName()))
          .schoolName(ds.getString(SCHOOL_NAME.getName()))
          .build());
    }else if(ds.isRecordID(TRAILER.getName())){
      batchFile.setBatchFileTrailer(BatchFileTrailer.builder()
          .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
          .productID(ds.getString(PRODUCT_ID.getName()))
          .productName(ds.getString(PRODUCT_NAME.getName()))
          .studentCount(ds.getString(STUDENT_COUNT.getName()))
          .vendorName(ds.getString(VENDOR_NAME.getName()))
          .build());
    }
  }
}
