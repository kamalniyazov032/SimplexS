package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.kassa.KassaEmeliyyatRepository;
import az.simplexs.simplexs.service.kassa.KassaEmeliyyatService;
import az.simplexs.simplexs.service.kassa.KassaEmeliyyatService.PaymentValidationException;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class KassaOdenishController {
    private static final Logger log=LoggerFactory.getLogger(KassaOdenishController.class);
    private static final String TOKENS="kassa.paymentTokens";
    private final KassaEmeliyyatRepository repo;
    private final KassaEmeliyyatService service;
    private final MessageSource messages;
    public KassaOdenishController(KassaEmeliyyatRepository repo,KassaEmeliyyatService service,MessageSource messages) {
        this.repo=repo;this.service=service;this.messages=messages;
    }
    @GetMapping({"/kassa","/kassaOdenish"})
    public String home(@RequestParam(required=false) Long kassaId,Authentication auth,HttpSession session,Model model) {
        var registers=service.cashRegisters(auth,clinic(session));
        if(kassaId!=null) service.requireCash(auth,clinic(session),kassaId,false);
        model.addAttribute("registers",registers);
        model.addAttribute("cashId",kassaId!=null?kassaId:registers.isEmpty()?null:KassaEmeliyyatService.id(registers.getFirst(),"kassa_id"));
        base(model,"kassa.title");
        return "pages/maliyye/kassa";
    }
    @GetMapping({"/kassa/odenis","/kassa/borc"})
    public String workspace(@RequestParam Long kassaId,@RequestParam(required=false) Long targetId,
            Authentication auth,HttpSession session,Model model,jakarta.servlet.http.HttpServletRequest request) {
        boolean debt=request.getServletPath().endsWith("/borc");
        Long clinic=clinic(session);
        var cash=service.requireCash(auth,clinic,kassaId,false);
        var patients=debt?repo.debtors(clinic,kassaId):repo.visits(clinic,kassaId);
        var selected=patients.stream().filter(r->Objects.equals(KassaEmeliyyatService.id(r,debt?"xeste_id":"gelis_id"),targetId)).findFirst().orElse(null);
        model.addAttribute("patients",patients);
        model.addAttribute("selected",selected);
        model.addAttribute("cash",cash);
        model.addAttribute("cashId",kassaId);
        model.addAttribute("debt",debt);
        model.addAttribute("targetId",selected==null?null:targetId);
        model.addAttribute("listPath",debt?"/kassa/borc":"/kassa/odenis");
        model.addAttribute("canWrite",Boolean.TRUE.equals(cash.get("islesin")));
        model.addAttribute("canBorrow",!debt && service.canBorrow(auth,clinic));
        model.addAttribute("services",selected==null?List.of():debt?repo.debtServices(clinic,kassaId,targetId):repo.services(clinic,kassaId,targetId));
        model.addAttribute("receipts",selected==null||debt?List.of():repo.receipts(clinic,kassaId,targetId));
        model.addAttribute("paymentTypes",selected==null?List.of():repo.paymentTypes());
        model.addAttribute("paymentToken",selected==null?"":issueToken(session,kassaId,targetId,debt));
        base(model,debt?"kassa.debtPayment":"kassa.patientPayment");
        return "pages/maliyye/kassa-odenis";
    }
    @PostMapping({"/kassa/odenis","/kassa/borc"})
    public String pay(@RequestParam Long kassaId,@RequestParam Long targetId,@RequestParam String paymentToken,
            @RequestParam(required=false) List<Long> serviceIds,@RequestParam(required=false) List<Long> paymentTypeIds,
            @RequestParam(required=false) List<BigDecimal> amounts,@RequestParam(defaultValue="false") boolean borrow,
            @RequestParam(required=false) String note,Authentication auth,HttpSession session,
            jakarta.servlet.http.HttpServletRequest request,RedirectAttributes flash) {
        boolean debt=request.getServletPath().endsWith("/borc");
        service.requireCash(auth,clinic(session),kassaId,true);
        String path=debt?"/kassa/borc":"/kassa/odenis";
        boolean success=false;
        try {
            // Consume before executing: a repeated click/replayed form cannot repeat a payment.
            consumeToken(session,paymentToken,kassaId,targetId,debt);
            var result=service.pay(auth,clinic(session),kassaId,targetId,debt,serviceIds,paymentTypeIds,amounts,borrow,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            String code=String.valueOf(result.get("status_kodu"));
            String key=success?"kassa.paymentSuccess":"kassa.result."+code;
            flash.addFlashAttribute(success?"successMessage":"errorMessage",messages.getMessage(key,null,msg("kassa.paymentFailed"),LocaleContextHolder.getLocale()));
            if(!success) log.warn("Cash payment rejected: status={}",code);
        } catch(PaymentValidationException e) {
            flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) {
            // Never auto-retry a financial write: the outcome may be unknown after a connection failure.
            log.error("Cash payment outcome requires verification",e);
            flash.addFlashAttribute("errorMessage",msg("kassa.paymentUnknown"));
        }
        flash.addAttribute("kassaId",kassaId);
        if(!success) flash.addAttribute("targetId",targetId);
        return "redirect:"+path;
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView denied() { return new ModelAndView("error/403",Map.of(),HttpStatus.FORBIDDEN); }
    private Long clinic(HttpSession session) { return (Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID); }
    private void base(Model model,String title) { model.addAttribute("pageTitle",msg(title));model.addAttribute("activeMenu","kassa"); }
    private String msg(String key) { return messages.getMessage(key,null,LocaleContextHolder.getLocale()); }
    private record PaymentTarget(Long cash,Long target,boolean debt) implements java.io.Serializable {}
    @SuppressWarnings("unchecked")
    private Map<String,PaymentTarget> tokens(HttpSession session) {
        var value=(Map<String,PaymentTarget>)session.getAttribute(TOKENS);
        if(value==null) { value=new LinkedHashMap<>();session.setAttribute(TOKENS,value); }
        return value;
    }
    private String issueToken(HttpSession session,Long cash,Long target,boolean debt) {
        synchronized(session) {
            var values=tokens(session);
            if(values.size()>=20) values.remove(values.keySet().iterator().next());
            String token=UUID.randomUUID().toString();values.put(token,new PaymentTarget(cash,target,debt));return token;
        }
    }
    private void consumeToken(HttpSession session,String token,Long cash,Long target,boolean debt) {
        synchronized(session) {
            if(!new PaymentTarget(cash,target,debt).equals(tokens(session).remove(token))) throw new PaymentValidationException("duplicatePayment");
        }
    }
}
