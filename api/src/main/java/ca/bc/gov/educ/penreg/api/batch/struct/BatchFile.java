package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Batch file.
 *
 * @author OM  The type Batch file.
 */
@Data
public class BatchFile {
  private BatchFileHeader batchFileHeader;
  private List<StudentDetails> studentDetails;
  private BatchFileTrailer batchFileTrailer;


  /**
   * Gets student details.
   *
   * @return the student details
   */
  public List<StudentDetails> getStudentDetails() {
    if(this.studentDetails == null){
      this.studentDetails = new ArrayList<>();
    }
    return this.studentDetails;
  }
}
