package ca.bc.gov.educ.penreg.api.struct.v1.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenRequestBatchSchoolErrorNotificationEntity extends BatchNotificationEntity {
  String fileName;
  String failReason;
  String dateTime;
}
