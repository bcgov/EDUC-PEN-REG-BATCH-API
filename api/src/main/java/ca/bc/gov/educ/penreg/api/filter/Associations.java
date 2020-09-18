package ca.bc.gov.educ.penreg.api.filter;

import lombok.Getter;

import javax.persistence.criteria.Join;
import java.util.*;

public class Associations {
  /**
   * The association names in the orderBy statement
   */
  @Getter
  private Set<String> sortAssociations = new HashSet<>();

  /**
   * The association names in the where condition
   */
  @Getter
  private List<String> searchAssociations = new ArrayList<>();

  /**
   * The count for joining association
   */
  private int joinedCount = 0;

  /**
   * The joined association names in the where condition
   */
  @Getter
  private Map<String, Join<Object, Object>> joinedSearchAssociations = new HashMap<>();

  public Join<Object, Object> countJoin(String associationName) {
    var join = joinedSearchAssociations.get(associationName);
    joinedCount++;
    if(searchAssociations.size() == joinedCount) {
      reset();
    }
    return join;
  }

  public int cacheJoin(String associationName, Join<Object, Object> join) {
    joinedSearchAssociations.put(associationName, join);
    return joinedCount;
  }

  public void reset() {
    joinedCount = 0;
    joinedSearchAssociations.clear();
  }
}
