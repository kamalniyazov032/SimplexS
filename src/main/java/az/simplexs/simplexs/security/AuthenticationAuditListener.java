package az.simplexs.simplexs.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {
    private final AccessService access;
    public AuthenticationAuditListener(AccessService access){this.access=access;}

    @EventListener
    public void success(AuthenticationSuccessEvent event){
        access.audit(event.getAuthentication(),null,"LOGIN",null,null,null,true);
    }

    @EventListener
    public void failure(AuthenticationFailureBadCredentialsEvent event){
        access.audit(event.getAuthentication(),null,"LOGIN",null,null,null,false);
    }
}
