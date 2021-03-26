package ca.bc.gov.educ.penreg.api.batch.validator;

import ca.bc.gov.educ.penreg.api.batch.exception.FileUnProcessableException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@Slf4j
public class PenRequestBatchFileValidatorTest {
  @Autowired
  private PenRequestBatchFileValidator penRequestBatchFileValidator;

  @Test
  public void testValidateFileForHeaderAndTrailer_givenInvalidFileHeader_shouldThrowException() throws IOException {
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_wrong_header.txt")).getFile());
      final byte[] bFile = Files.readAllBytes(file.toPath());
      final Optional<Reader> batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(bFile)));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      final String errorMessage = "Invalid transaction code on Header record. It must be FFI";
      val result = Assertions.assertThrows(FileUnProcessableException.class, () -> this.penRequestBatchFileValidator.validateFileForFormatAndLength(UUID.randomUUID().toString(), ds), errorMessage);
      assertThat(result.getReason()).isEqualTo(errorMessage);
    }
  }

  @Test
  public void testValidateFileForHeaderAndTrailer_givenInvalidFileHeaderAndLength_shouldThrowException() throws IOException {
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_wrong_header_and_length.txt")).getFile());
      final byte[] bFile = Files.readAllBytes(file.toPath());
      final Optional<Reader> batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(bFile)));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      final String errorMessage = "Invalid transaction code on Header record. It must be FFI";
      val result = Assertions.assertThrows(FileUnProcessableException.class, () -> this.penRequestBatchFileValidator.validateFileForFormatAndLength(UUID.randomUUID().toString(), ds), errorMessage);
      assertThat(result.getReason()).isEqualTo(errorMessage);
    }
  }

  @Test
  public void testValidateFileForHeaderAndTrailer_givenInvalidFileTrailer_shouldThrowException() throws IOException {
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      final File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_wrong_trailer.txt")).getFile());
      final byte[] bFile = Files.readAllBytes(file.toPath());
      final Optional<Reader> batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(bFile)));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      final String errorMessage = "Invalid transaction code on Trailer record. It must be BTR";
      val result = Assertions.assertThrows(FileUnProcessableException.class, () -> this.penRequestBatchFileValidator.validateFileForFormatAndLength(UUID.randomUUID().toString(), ds));
      assertThat(result.getReason()).isEqualTo(errorMessage);
    }
  }

  @Test
  public void testValidateFileForHeaderAndTrailer_givenInvalidFileWithDetailRecordAtPosition2_shouldThrowException() throws IOException {
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      final File file =
          new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sample_wrong_details_record_length_at_position_2.txt")).getFile());
      final byte[] bFile = Files.readAllBytes(file.toPath());
      final Optional<Reader> batchFileReaderOptional = Optional.of(new InputStreamReader(new ByteArrayInputStream(bFile)));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      final String errorMessage = "Detail record 2 is missing characters.";
      val result = Assertions.assertThrows(FileUnProcessableException.class, () -> this.penRequestBatchFileValidator.validateFileForFormatAndLength(UUID.randomUUID().toString(), ds));
      assertThat(result.getReason()).isEqualTo(errorMessage);
    }
  }
}
