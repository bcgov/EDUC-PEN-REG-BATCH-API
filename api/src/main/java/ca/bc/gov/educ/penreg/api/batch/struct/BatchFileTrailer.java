package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  @author OM
 * The type Batch file trailer.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchFileTrailer {
  private  String transactionCode; // TRANSACTION_CODE	3	0	Always "BTR"
  private  String studentCount; // STUDENT_ COUNT	6	3	The number of student records (SRM records) included in the PEN REQUEST file.  This value is calculated by the software that creates the PEN REQUEST. The field is right-justified with leading zeros.
  private  String vendorName; // VENDOR_NAME	100	9	The name of the software vendor.
  private  String productName; // PRODUCT_NAME	100	109	The name of the software product that was used.
  private  String productID; // PRODUCT_ID	15	209	Unused

}