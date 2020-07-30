package ca.bc.gov.educ.penreg.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Pen request batch.
 *  @author OM
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S1700")
public class PenRequestBatch {
  /**
   * The Pen request batch id.
   */
  String penRequestBatchID;
  /**
   * The Submission number.
   */
  String submissionNumber;
  /**
   * The Pen request batch status code.
   */
  String penRequestBatchStatusCode;
  /**
   * The Pen request batch status reason.
   */
  String penRequestBatchStatusReason;
  /**
   * The Pen request batch type code.
   */
  String penRequestBatchTypeCode;
  /**
   * The Unarchived flag.
   */
  String unarchivedFlag;
  /**
   * The Unarchived batch changed flag.
   */
  String unarchivedBatchChangedFlag;
  /**
   * The File name.
   */
  String fileName;
  /**
   * The File type.
   */
  String fileType;
  /**
   * The Insert date.
   */
  String insertDate;
  /**
   * The Extract date.
   */
  String extractDate;
  /**
   * The Process date.
   */
  String processDate;
  /**
   * The Source application.
   */
  String sourceApplication;
  /**
   * The Pen request batch source code.
   */
  String ministryPRBSourceCode;
  /**
   * The Tsw account.
   */
  String tswAccount;
  /**
   * The Min code.
   */
  String minCode;
  /**
   * The School name.
   */
  String schoolName;
  /**
   * The Contact name.
   */
  String contactName;
  /**
   * The Email.
   */
  String email;
  /**
   * The Office number.
   */
  String officeNumber;
  /**
   * The Source student count.
   */
  String sourceStudentCount;
  /**
   * The Student count.
   */
  String studentCount;

  String issuedPenCount;
  /**
   * The Error count.
   */
  String errorCount;
  /**
   * The Matched count.
   */
  String matchedCount;
  /**
   * The Repeat count.
   */
  String repeatCount;
  /**
   * The Fixable count.
   */
  String fixableCount;
  /**
   * The Sis vendor name.
   */
  String sisVendorName;
  /**
   * The Sis product name.
   */
  String sisProductName;
  /**
   * The Sis product id.
   */
  String sisProductID;
}
