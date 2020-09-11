package ca.bc.gov.educ.penreg.api.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The type Pen reg api mvc config.
 *
 * @author Om
 */
@Configuration
public class PenRegAPIMVCConfig implements WebMvcConfigurer {

  /**
   * The Pen reg api interceptor.
   */
  @Getter(AccessLevel.PRIVATE)
  private final PenRegAPIInterceptor penRegAPIInterceptor;

  /**
   * Instantiates a new Pen reg api mvc config.
   *
   * @param penRegAPIInterceptor the pen reg api interceptor
   */
  @Autowired
  public PenRegAPIMVCConfig(final PenRegAPIInterceptor penRegAPIInterceptor) {
    this.penRegAPIInterceptor = penRegAPIInterceptor;
  }

  /**
   * Add interceptors.
   *
   * @param registry the registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(penRegAPIInterceptor).addPathPatterns("/**/**/");
  }
}
