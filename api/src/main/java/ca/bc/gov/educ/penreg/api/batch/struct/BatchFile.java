package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 *  @author OM
 * The type Batch file.
 */
@Data
public class BatchFile {
  private BatchFileHeader batchFileHeader;
  private Set<StudentDetails> studentDetails;
  private BatchFileTrailer batchFileTrailer;


  public Set<StudentDetails> getStudentDetails() {
    if(this.studentDetails == null){
      this.studentDetails = new HashSet<>();
    }
    return this.studentDetails;
  }
}
