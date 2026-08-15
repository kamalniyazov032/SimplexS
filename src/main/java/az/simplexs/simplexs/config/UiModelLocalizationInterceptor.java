package az.simplexs.simplexs.config;

import java.util.*;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UiModelLocalizationInterceptor implements HandlerInterceptor {
    private static final Set<String> ATTRIBUTES=Set.of("pageTitle","successMessage","errorMessage","errorTitle","errorDescription","errorAction");
    private final MessageSource messages;private final Map<String,String> keysByAzerbaijani;
    public UiModelLocalizationInterceptor(MessageSource messages){this.messages=messages;var bundle=ResourceBundle.getBundle("messages",Locale.forLanguageTag("az"));var map=new HashMap<String,String>();bundle.keySet().forEach(k->map.putIfAbsent(bundle.getString(k),k));keysByAzerbaijani=Map.copyOf(map);}
    @Override public void postHandle(HttpServletRequest request,HttpServletResponse response,Object handler,ModelAndView view){
        if(view==null)return;Locale locale=org.springframework.web.servlet.support.RequestContextUtils.getLocale(request);
        ATTRIBUTES.forEach(name->{Object value=view.getModel().get(name);if(value instanceof String text){String key=keysByAzerbaijani.get(text);if(key!=null)view.addObject(name,messages.getMessage(key,null,text,locale));}});
    }
}
