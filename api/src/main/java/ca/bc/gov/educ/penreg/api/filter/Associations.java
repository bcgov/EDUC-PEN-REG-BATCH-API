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
  @Getter
  private int joinedCount = 0;

  /**
   * The joined association names in the where condition
   */
  @Getter
  private Map<String, Join<Object, Object>> joinedSearchAssociations = new HashMap<>();

  /**
   * Count the join operation and return the cached join operation
   * also reset the count and remove the cache when all associations are joined because Hibernate may run another count query to determine the number of results
   *
   * @param associationName  the association name
   * @return join            the cached join operation
   */
  public Join<Object, Object> countJoin(String associationName) {
    var join = joinedSearchAssociations.get(associationName);
    joinedCount++;
    return join;
  }

  /**
   * Cache the join operation because we just need one join operation for one association
   * @param associationName the association name
   * @param join            the join operation
   * @return the count for the join operation
   */
  public int cacheJoin(String associationName, Join<Object, Object> join) {
    joinedSearchAssociations.put(associationName, join);
    return joinedCount;
  }

  /**
   * reset the count and remove the cache if all joins are processed
   */
  public void resetIfAllJoinsProcessed() {
    if(searchAssociations.size() == joinedCount) {
      reset();
    }
  }

  /**
   * reset the count and remove the cache
   */
  public void reset() {
    joinedCount = 0;
    joinedSearchAssociations.clear();
  }
}
