package az.simplexs.simplexs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UiWebConfig implements WebMvcConfigurer {
    private final UiModelLocalizationInterceptor localization;
    public UiWebConfig(UiModelLocalizationInterceptor localization){this.localization=localization;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(localization);}
}
