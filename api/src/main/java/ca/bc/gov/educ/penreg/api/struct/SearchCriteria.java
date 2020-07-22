package ca.bc.gov.educ.penreg.api.struct;

import ca.bc.gov.educ.penreg.api.filter.FilterOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SearchCriteria {
  @NotNull
  String key;
  @NotNull
  FilterOperation operation;
  String value;
  @NotNull
  ValueType valueType;
}
