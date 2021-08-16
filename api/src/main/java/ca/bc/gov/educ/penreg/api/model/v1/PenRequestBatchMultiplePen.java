package ca.bc.gov.educ.penreg.api.model.v1;

import java.util.List;

/**
 * This is a projection interface used to get the native query results.
 * this is used in this repository method.
 * {@link ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepository#findBatchFilesWithMultipleAssignedPens(List)}
 * The implementation class will be delivered by Spring boot magically.
 * for more information look at the below class.
 * {@link org.springframework.data.projection.ProjectionFactory}
 *
 * @author om
 */
public interface PenRequestBatchMultiplePen {

  /**
   * @return the submission number for which, same pen is assigned to multiple pen requests either by system or by the user.
   */
  String getSubmissionNumber();

}
