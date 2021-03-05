package ca.bc.gov.educ.penreg.api.batch.service;

import ca.bc.gov.educ.penreg.api.constants.SchoolGroupCodes;
import ca.bc.gov.educ.penreg.api.model.v1.PENWebBlobEntity;

public interface DuplicateFileCheckService {


  boolean isBatchFileDuplicate(PENWebBlobEntity penWebBlobEntity);

  SchoolGroupCodes getSchoolGroupCode();
}
