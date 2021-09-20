package ca.bc.gov.educ.penreg.api.service.interfaces;

import ca.bc.gov.educ.penreg.api.constants.SchoolTypeCode;

public interface PenMatchResultProcessingService<T, R> {
  R processPenMatchResults(T t);

  SchoolTypeCode getSchoolTypeCode();
}
