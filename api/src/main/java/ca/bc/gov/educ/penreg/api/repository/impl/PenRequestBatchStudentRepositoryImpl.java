package ca.bc.gov.educ.penreg.api.repository.impl;

import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchStudentRepositoryCustom;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestIDs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Repository
public class PenRequestBatchStudentRepositoryImpl implements PenRequestBatchStudentRepositoryCustom {

  @Getter(AccessLevel.PRIVATE)
  private final EntityManager entityManager;

  /**
   * Instantiates a new pen request batch student repository custom.
   *
   * @param em the entity manager
   */
  @Autowired
  PenRequestBatchStudentRepositoryImpl(final EntityManager em) {
    this.entityManager = em;
  }


  @Override
  public List<PenRequestIDs> getAllPenRequestBatchStudentIDs(List<UUID> penRequestBatchIDs, List<String> penRequestBatchStudentStatusCodes, Map<String,String> searchCriteria) {

    StringBuilder sqlString = new StringBuilder();
    sqlString.append("SELECT s.PEN_REQUEST_BATCH_STUDENT_ID, s.PEN_REQUEST_BATCH_ID FROM PEN_REQUEST_BATCH_STUDENT s");
    sqlString.append(" LEFT JOIN PEN_REQUEST_BATCH b ON b.PEN_REQUEST_BATCH_ID = s.PEN_REQUEST_BATCH_ID");
    sqlString.append(" WHERE s.PEN_REQUEST_BATCH_ID IN (:batchIDs) AND s.PEN_REQUEST_BATCH_STUDENT_STATUS_CODE IN (:statusCodes)");

    if(searchCriteria != null) {
      searchCriteria.forEach((key, value) -> {
        switch (key) {
          case ("mincode"):
            sqlString.append(" AND b.MINCODE = :mincode");
            break;
          case ("legalSurname"):
            sqlString.append(" AND s.LEGAL_LAST_NAME LIKE :legalSurname");
            break;
          case ("legalGivenName"):
            sqlString.append(" AND s.LEGAL_FIRST_NAME LIKE :legalGivenName");
            break;
          case ("legalMiddleNames"):
            sqlString.append(" AND s.LEGAL_MIDDLE_NAMES LIKE :legalMiddleNames");
            break;
          case ("usualSurname"):
            sqlString.append(" AND s.USUAL_LAST_NAME LIKE :usualSurname");
            break;
          case ("usualGivenName"):
            sqlString.append(" AND s.USUAL_FIRST_NAME LIKE :usualGivenName");
            break;
          case ("usualMiddleNames"):
            sqlString.append(" AND s.USUAL_MIDDLE_NAMES LIKE :usualMiddleNames");
            break;
          case ("gender"):
            sqlString.append(" AND s.GENDER_CODE LIKE :gender");
            break;
          case ("dob"):
            sqlString.append(" AND s.DOB LIKE :dob");
            break;
          case ("bestMatchPEN"):
            sqlString.append(" AND s.BEST_MATCH_PEN LIKE :bestMatchPEN");
            break;
          case ("submissionNumber"):
            sqlString.append(" AND b.SUBMISSION_NO LIKE :submissionNumber");
            break;
          case ("localID"):
            sqlString.append(" AND s.LOCAL_ID LIKE :localID");
            break;
          case ("postalCode"):
            sqlString.append(" AND s.POSTAL_CODE LIKE :postalCode");
            break;
          case ("grade"):
            sqlString.append(" AND s.GRADE_CODE LIKE :grade");
            break;
          case ("submittedPen"):
            sqlString.append(" AND s.SUBMITTED_PEN LIKE :submittedPen");
            break;
          default:
            log.error("Unknown search criteria key provided for pen request batch ids search. It is being ignored :: " + key);
            break;
        }
      });
    }

    sqlString.append(" ORDER BY b.MINCODE ASC," +
      " b.SUBMISSION_NO ASC," +
      " s.RECORD_NUMBER ASC");

    Query q = this.entityManager.createNativeQuery(sqlString.toString(), "penRequestIDsMapping");
    q.setParameter("batchIDs", penRequestBatchIDs);
    q.setParameter("statusCodes", penRequestBatchStudentStatusCodes);
    if(searchCriteria != null) {
      searchCriteria.forEach(q::setParameter);
    }

    return q.getResultList();
  }
}
