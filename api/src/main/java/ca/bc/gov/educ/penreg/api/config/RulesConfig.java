package ca.bc.gov.educ.penreg.api.config;

import ca.bc.gov.educ.penreg.api.repository.PenRequestBatchValidOneLetterGivenNameCodeRepository;
import ca.bc.gov.educ.penreg.api.validation.rules.Rule;
import ca.bc.gov.educ.penreg.api.validation.rules.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class RulesConfig {


  @Bean
  @Order(1)
  public Rule submittedPENRule() {
    return new SubmittedPENRule();
  }

  @Bean
  @Order(2)
  public Rule legalLastNameRule() {
    return new LegalLastNameRule();
  }

  @Bean
  @Autowired
  @Order(3)
  public Rule legalFirstNameRule(PenRequestBatchValidOneLetterGivenNameCodeRepository repository) {
    return new LegalFirstNameRule(repository);
  }

  @Bean
  @Order(4)
  public Rule legalMiddleNameRule() {
    return new LegalMiddleNameRule();
  }

  @Bean
  @Order(5)
  public Rule usualMiddleNameRule() {
    return new UsualMiddleNameRule();
  }


}
