package ca.bc.gov.educ.penreg.api.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PenRegAPIMVCConfig implements WebMvcConfigurer {

    @Getter(AccessLevel.PRIVATE)
    private final PenRegAPIInterceptor penRegAPIInterceptor;

    @Autowired
    public PenRegAPIMVCConfig(final PenRegAPIInterceptor penRegAPIInterceptor){
        this.penRegAPIInterceptor = penRegAPIInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(penRegAPIInterceptor).addPathPatterns("/**/**/");
    }
}
