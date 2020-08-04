package ca.bc.gov.educ.penreg.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Batch file header.
 *
 * @author OM  The type Batch file header.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchFileHeader {
  private String transactionCode; //TRANSACTION_CODE	3	0	Always "FFI"
  private String minCode; // MINISTRY_SCHOOL_CODE	8	3	An 8 digit number that uniquely identifies a school.  The number is assigned by the Ministry.
  private String schoolName; //SCHOOL_NAME	40	11	The name of the school.
  private String requestDate; //  REQUEST_DATE	8	51	The day on which the PEN REQUEST file was created. Format: YYYYMMDD
  private String emailID; // EMAIL_ID	100	59	Email address of requesting office.
  private String faxNumber; // FAX_NUMBER	10	159	The 9 digit number including area code
  private String contactName; // CONTACT_NAME	40	169
  private String officeNumber; // OFFICE_NUMBER	2	209	Only complete if the school has multiple sites that request PENS and have been assigned an office # by the Ministry.
}
