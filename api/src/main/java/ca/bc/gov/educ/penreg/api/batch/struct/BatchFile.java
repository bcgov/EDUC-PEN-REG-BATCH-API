package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author OM
 * The type Batch file.
 */
@Data
public class BatchFile {
  private BatchFileHeader batchFileHeader;
  private List<StudentDetails> studentDetails;
  private BatchFileTrailer batchFileTrailer;


  public List<StudentDetails> getStudentDetails() {
    if(this.studentDetails == null){
      this.studentDetails = new ArrayList<>();
    }
    return this.studentDetails;
  }
}
