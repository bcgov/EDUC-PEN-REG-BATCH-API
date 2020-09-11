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
  /**
   * The Min code.
   */
  private String minCode; // MINISTRY_SCHOOL_CODE	8	3	An 8 digit number that uniquely identifies a school.  The number is assigned by the Ministry.
  /**
   * The School name.
   */
  private String schoolName; //SCHOOL_NAME	40	11	The name of the school.
  /**
   * The Request date.
   */
  private String requestDate; //  REQUEST_DATE	8	51	The day on which the PEN REQUEST file was created. Format: YYYYMMDD
  /**
   * The Email id.
   */
  private String emailID; // EMAIL_ID	100	59	Email address of requesting office.
  /**
   * The Fax number.
   */
  private String faxNumber; // FAX_NUMBER	10	159	The 9 digit number including area code
  /**
   * The Contact name.
   */
  private String contactName; // CONTACT_NAME	40	169
  /**
   * The Office number.
   */
  private String officeNumber; // OFFICE_NUMBER	2	209	Only complete if the school has multiple sites that request PENS and have been assigned an office # by the Ministry.
}
