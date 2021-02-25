package ca.bc.gov.educ.penreg.api.struct.v1.notification;

import lombok.Data;

@Data
public abstract class BatchNotificationEntity {
  String fromEmail;
  String toEmail;
  String submissionNumber;
}
