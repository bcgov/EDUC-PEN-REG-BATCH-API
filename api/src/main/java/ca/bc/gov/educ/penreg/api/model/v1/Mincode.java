package ca.bc.gov.educ.penreg.api.model.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * The type Mincode.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Mincode implements Serializable {
  private static final long serialVersionUID = -4155361546727228402L;
  /**
   * The Dist no.
   */
  @Column(name = "DISTNO", nullable = false, length = 3)
  Integer districtNumber;

  /**
   * The Schl no.
   */
  @Column(name = "SCHLNO", nullable = false, length = 5)
  Integer schoolNumber;
}
