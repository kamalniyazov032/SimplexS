package az.simplexs.simplexs.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import az.simplexs.simplexs.controller.KlinikaController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ModuleAccessFilter extends OncePerRequestFilter {
    private final AccessService access;
    public ModuleAccessFilter(AccessService access){this.access=access;}

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof AuthenticatedPersonal){
            Long clinicId=(Long)request.getSession().getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
            if(clinicId==null){clinicId=access.firstClinicId(auth);if(clinicId!=null)request.getSession().setAttribute(KlinikaController.SELECTED_KLINIKA_ID,clinicId);}
            String path=request.getRequestURI().substring(request.getContextPath().length());
            if(access.isRegisteredRoute(path)&&!access.canAccessRoute(auth,clinicId,path)){
                access.audit(auth,clinicId,"MODUL_GIRISI_REDDEDILDI",path,request.getMethod(),request.getRemoteAddr(),false);
                forbidden(request,response);return;
            }
            String permission=access.requiredPermission(path,request.getMethod());
            if(permission!=null&&!access.hasPermission(auth,clinicId,permission)){
                access.audit(auth,clinicId,"EMELIYYAT_REDDEDILDI",path,request.getMethod(),request.getRemoteAddr(),false);
                forbidden(request,response);return;
            }
        }
        chain.doFilter(request,response);
    }

    private void forbidden(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI,request.getRequestURI());
        request.getRequestDispatcher("/error").forward(request,response);
    }
}
