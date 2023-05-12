package ca.bc.gov.educ.penreg.api.repository;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestIDs;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PenRequestBatchStudentRepositoryCustom {

  /**
   * Custom query to return only ids
   *
   * @param penRequestBatchIDs - the list of ids
   * @param penRequestBatchStudentStatusCodes - the list of status codes
   * @param searchCriteria - the criteria used to filter requests
   * @return - the list of ids
   */
  List<PenRequestIDs> getAllPenRequestBatchStudentIDs(List<UUID> penRequestBatchIDs,
                                                      List<String> penRequestBatchStudentStatusCodes,
                                                      Map<String,String> searchCriteria);
}
