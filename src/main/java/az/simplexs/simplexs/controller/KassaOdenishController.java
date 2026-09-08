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
        model.addAttribute("paymentToken",selected==null?"":issueToken(session,kassaId,targetId,debt ? "BORC" : "ODENIS"));
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
            consumeToken(session,paymentToken,kassaId,targetId,debt ? "BORC" : "ODENIS");
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
    @GetMapping({"/kassa/diger-giris","/kassa/avans"})
    public String otherIncome(@RequestParam Long kassaId, Authentication auth, HttpSession session, Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        boolean advance=request.getRequestURI().endsWith("/avans");
        var cash=service.requireCash(auth,clinic(session),kassaId,true);
        home(kassaId,auth,session,model);
        model.addAttribute("incomeCash",cash);
        model.addAttribute("incomeAccounts",repo.accountingCodes(clinic(session),advance?"AVANS_QEBUL":"DIGER_GIRIS"));
        model.addAttribute("incomePaymentTypes",repo.paymentTypes());
        model.addAttribute("incomeToken",issueToken(session,kassaId,null,advance?"AVANS_QEBUL":"DIGER_GIRIS"));
        model.addAttribute("showOtherIncome",true);
        model.addAttribute("advanceMode",advance);
        if(advance) {
            model.addAttribute("advanceCardTypes",repo.cardTypes());
            Long selectedVisit=(Long)model.getAttribute("advanceVisitId");
            var selected=selectedVisit==null?Map.<String,Object>of():repo.advanceVisit(clinic(session),selectedVisit);
            model.addAttribute("advanceSelected",selected);
        }
        return "pages/maliyye/kassa";
    }
    @PostMapping({"/kassa/diger-giris","/kassa/avans"})
    public String saveOtherIncome(@RequestParam Long kassaId, @RequestParam String paymentToken,
            @RequestParam(required=false) Long accountId, @RequestParam(required=false) List<Long> paymentTypeIds,
            @RequestParam(required=false) List<BigDecimal> amounts, @RequestParam(required=false) String note,
            @RequestParam(required=false) Long visitId, jakarta.servlet.http.HttpServletRequest request,
            Authentication auth, HttpSession session, RedirectAttributes flash) {
        boolean advance=request.getRequestURI().endsWith("/avans");
        service.requireCash(auth,clinic(session),kassaId,true);
        boolean success=false;
        try {
            consumeToken(session,paymentToken,kassaId,null,advance?"AVANS_QEBUL":"DIGER_GIRIS");
            var result=advance?service.advance(auth,clinic(session),kassaId,visitId,accountId,paymentTypeIds,amounts,note)
                    :service.otherIncome(auth,clinic(session),kassaId,accountId,paymentTypeIds,amounts,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            String code=String.valueOf(result.get("status_kodu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",messages.getMessage(
                    success?(advance?"kassa.advanceSuccess":"kassa.incomeSuccess"):"kassa.result."+code,null,msg("kassa.incomeFailed"),LocaleContextHolder.getLocale()));
            if(!success) log.warn("Other cash income rejected: status={}",code);
        } catch(PaymentValidationException e) {
            flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) {
            log.error("Other cash income outcome requires verification",e);
            flash.addFlashAttribute("errorMessage",msg("kassa.paymentUnknown"));
        }
        flash.addAttribute("kassaId",kassaId);
        if(!success) {
            if(advance) flash.addFlashAttribute("advanceVisitId",visitId);
            flash.addFlashAttribute("incomeAccountId",accountId);
            flash.addFlashAttribute("incomeNote",note);
            Map<Long,BigDecimal> entered=new LinkedHashMap<>();
            if(paymentTypeIds!=null && amounts!=null && paymentTypeIds.size()==amounts.size()) {
                for(int i=0;i<paymentTypeIds.size();i++) entered.put(paymentTypeIds.get(i),amounts.get(i));
            }
            flash.addFlashAttribute("incomeAmounts",entered);
        }
        return success?"redirect:/kassa":advance?"redirect:/kassa/avans":"redirect:/kassa/diger-giris";
    }
    @GetMapping("/kassa/avans/xesteler")
    public String advancePatients(@RequestParam Long kassaId,@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) String cardType,@RequestParam(defaultValue="1") int page,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            Authentication auth,HttpSession session,Model model) {
        service.requireCash(auth,clinic(session),kassaId,true);
        String query=q.trim();
        String card=cardType==null || cardType.isBlank()?null:cardType.trim();
        boolean invalid=page<1 || query.length()>200 || (from!=null && to!=null && from.isAfter(to))
                || (card!=null && repo.cardTypes().stream().noneMatch(r->card.equals(r.get("kod"))));
        if(invalid) {
            model.addAttribute("advanceSearchError",msg("kassa.invalidAdvanceSearch"));
            model.addAttribute("advanceResults",List.of());
        } else {
            var rows=query.isEmpty() && card==null && from==null && to==null?List.<Map<String,Object>>of()
                    :repo.advancePatients(clinic(session),card,query,from,to,page);
            model.addAttribute("advanceResults",rows.stream().limit(100).toList());
            model.addAttribute("advanceHasMore",rows.size()>100);
        }
        model.addAttribute("advancePage",Math.max(1,page));
        return "fragments/kassa-advance-search :: results";
    }
    @GetMapping("/kassa/qaytarma")
    public String refunds(@RequestParam Long kassaId,@RequestParam(required=false) Long targetId,
            @RequestParam(defaultValue="") String q,@RequestParam(required=false) String cardType,
            @RequestParam(defaultValue="1") int page,Authentication auth,HttpSession session,Model model) {
        var cash=service.requireCash(auth,clinic(session),kassaId,false);
        String card=cardType==null || cardType.isBlank()?null:cardType.trim();
        var cards=repo.cardTypes();
        boolean invalid=page<1 || q.length()>200 || (card!=null && cards.stream().noneMatch(r->card.equals(r.get("kod"))));
        var patients=invalid?List.<Map<String,Object>>of():repo.refundPatients(clinic(session),kassaId,card,q.trim(),page);
        var services=targetId==null?List.<Map<String,Object>>of():repo.refundServices(clinic(session),kassaId,targetId);
        if(invalid) model.addAttribute("errorMessage",msg("kassa.invalidAdvanceSearch"));
        if(targetId!=null && services.isEmpty()) model.addAttribute("errorMessage",msg("kassa.refundStale"));
        model.addAttribute("cash",cash);model.addAttribute("cashId",kassaId);
        model.addAttribute("patients",patients.stream().limit(100).toList());model.addAttribute("hasMore",patients.size()>100);
        model.addAttribute("cardTypes",cards);model.addAttribute("cardType",card);model.addAttribute("query",q);model.addAttribute("page",Math.max(1,page));
        model.addAttribute("services",services);model.addAttribute("selected",services.isEmpty()?null:services.getFirst());
        model.addAttribute("targetId",targetId);model.addAttribute("canWrite",Boolean.TRUE.equals(cash.get("islesin")));
        model.addAttribute("refundAccounts",services.isEmpty()?List.of():repo.accountingCodes(clinic(session),"XESTE_QAYTARMA"));
        model.addAttribute("paymentTypes",services.isEmpty()?List.of():repo.paymentTypes());
        model.addAttribute("paymentToken",services.isEmpty()?"":issueToken(session,kassaId,targetId,"XESTE_QAYTARMA"));
        base(model,"kassa.refund");
        return "pages/maliyye/kassa-qaytarma";
    }
    @PostMapping("/kassa/qaytarma")
    public String refund(@RequestParam Long kassaId,@RequestParam Long targetId,@RequestParam String paymentToken,
            @RequestParam(required=false) List<Long> serviceIds,@RequestParam(required=false) Long accountId,
            @RequestParam(required=false) Long paymentTypeId,@RequestParam(required=false) BigDecimal expectedTotal,
            @RequestParam(required=false) String note,Authentication auth,HttpSession session,RedirectAttributes flash) {
        service.requireCash(auth,clinic(session),kassaId,true);
        boolean success=false;
        try {
            consumeToken(session,paymentToken,kassaId,targetId,"XESTE_QAYTARMA");
            var result=service.refund(auth,clinic(session),kassaId,targetId,serviceIds,accountId,paymentTypeId,expectedTotal,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",msg(success?"kassa.refundSuccess":"kassa.refundFailed"));
            if(!success) log.warn("Patient refund rejected: status={}",result.get("status_kodu"));
        } catch(PaymentValidationException e) {
            flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) {
            log.error("Patient refund outcome requires verification",e);
            flash.addFlashAttribute("errorMessage",msg("kassa.refundUnknown"));
        }
        flash.addAttribute("kassaId",kassaId);
        if(!success) {
            flash.addAttribute("targetId",targetId);
            flash.addFlashAttribute("refundAccountId",accountId);flash.addFlashAttribute("refundPaymentTypeId",paymentTypeId);
            flash.addFlashAttribute("refundNote",note);flash.addFlashAttribute("refundSelection",serviceIds==null?List.of():serviceIds);
        }
        return "redirect:/kassa/qaytarma";
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView denied() { return new ModelAndView("error/403",Map.of(),HttpStatus.FORBIDDEN); }
    private Long clinic(HttpSession session) { return (Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID); }
    private void base(Model model,String title) { model.addAttribute("pageTitle",msg(title));model.addAttribute("activeMenu","kassa"); }
    private String msg(String key) { return messages.getMessage(key,null,LocaleContextHolder.getLocale()); }
    private record PaymentTarget(Long clinic,Long cash,Long target,String operation) implements java.io.Serializable {}
    @SuppressWarnings("unchecked")
    private Map<String,PaymentTarget> tokens(HttpSession session) {
        var value=(Map<String,PaymentTarget>)session.getAttribute(TOKENS);
        if(value==null) { value=new LinkedHashMap<>();session.setAttribute(TOKENS,value); }
        return value;
    }
    private String issueToken(HttpSession session,Long cash,Long target,String operation) {
        synchronized(session) {
            var values=tokens(session);
            if(values.size()>=20) values.remove(values.keySet().iterator().next());
            String token=UUID.randomUUID().toString();values.put(token,new PaymentTarget(clinic(session),cash,target,operation));return token;
        }
    }
    private void consumeToken(HttpSession session,String token,Long cash,Long target,String operation) {
        synchronized(session) {
            if(!new PaymentTarget(clinic(session),cash,target,operation).equals(tokens(session).remove(token))) throw new PaymentValidationException("duplicatePayment");
        }
    }
}
