package ca.bc.gov.educ.penreg.api.struct.v1.notification;

import java.util.*;
import lombok.Data;

@Data
public abstract class BatchNotificationEntity {
  String fromEmail;
  List<String> toEmail;
  String submissionNumber;
}
