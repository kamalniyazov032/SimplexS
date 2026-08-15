package az.simplexs.simplexs.config;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;

@Configuration
public class I18nConfig implements WebMvcConfigurer {
    @Bean public static LocaleChangeInterceptor localeChangeInterceptor(){var i=new LocaleChangeInterceptor();i.setParamName("lang");return i;}
    @Bean public LocaleResolver localeResolver(){var r=new SessionLocaleResolver();r.setDefaultLocale(Locale.forLanguageTag("az"));return r;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(localeChangeInterceptor());}

    @Bean(name="messageSource")
    public MessageSource messageSource(TercumeRepository repo){
        var files=new ResourceBundleMessageSource();files.setBasenames("messages");files.setDefaultEncoding("UTF-8");files.setFallbackToSystemLocale(false);
        return new AbstractMessageSource(){
            private final Map<String,Map<String,String>> cache=new ConcurrentHashMap<>();private volatile long version=-1;
            @Override protected MessageFormat resolveCode(String code,Locale locale){
                if(version!=repo.version()){cache.clear();version=repo.version();}
                String value=cache.computeIfAbsent(locale.getLanguage(),repo::tercumeler).get(code);
                if(value==null||value.isBlank())value=files.getMessage(code,null,null,locale);
                return value==null?null:new MessageFormat(value,locale);
            }
        };
    }
}
