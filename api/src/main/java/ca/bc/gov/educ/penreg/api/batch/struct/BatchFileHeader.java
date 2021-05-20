package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Batch file header.
 *
 * @author OM The type Batch file header.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchFileHeader {
  /**
   * The Transaction code.
   */
  private String transactionCode; //TRANSACTION_CODE	3	0	Always "FFI"
}
