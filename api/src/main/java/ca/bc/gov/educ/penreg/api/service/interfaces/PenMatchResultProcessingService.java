package ca.bc.gov.educ.penreg.api.service.interfaces;

public interface PenMatchResultProcessingService<T, R> {
  R processPenMatchResults(T t);
}
