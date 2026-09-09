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
    @GetMapping({"/kassa/balans","/kassa/balans-tarix"})
    public String balance(@RequestParam Long kassaId,@RequestParam(required=false) String from,
            @RequestParam(required=false) String to,Authentication auth,HttpSession session,Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        var cash=service.requireCash(auth,clinic(session),kassaId,false);
        home(kassaId,auth,session,model);
        boolean dated=request.getRequestURI().endsWith("/balans-tarix");
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku"));
        model.addAttribute("balanceCash",cash);model.addAttribute("showBalance",true);
        model.addAttribute("datedBalance",dated);
        model.addAttribute("balanceFrom",from==null?today.toString():from);
        model.addAttribute("balanceTo",to==null?today.toString():to);
        try {
            if(dated) {
                var start=from==null?today:java.time.LocalDate.parse(from);
                var end=to==null?today:java.time.LocalDate.parse(to);
                if(start.isAfter(end)) throw new java.time.DateTimeException("Invalid date range");
                model.addAttribute("balance",repo.balanceByDate(clinic(session),kassaId,start,end));
            } else model.addAttribute("balance",repo.balance(clinic(session),kassaId));
        } catch(java.time.DateTimeException e) {
            model.addAttribute("balanceError",msg("kassa.balanceInvalidDates"));
        } catch(DataAccessException e) {
            log.error("Cash balance lookup failed",e);
            model.addAttribute("balanceError",msg("kassa.balanceFailed"));
        }
        return "pages/maliyye/kassa";
    }
    @GetMapping("/kassa/transfer")
    public String transferPage(@RequestParam Long kassaId,Authentication auth,HttpSession session,Model model) {
        var cash=service.requireCash(auth,clinic(session),kassaId,true);
        home(kassaId,auth,session,model);
        model.addAttribute("showTransfer",true);model.addAttribute("transferCash",cash);
        var balance=KassaEmeliyyatService.money(repo.balance(clinic(session),kassaId),"cari_balans");
        model.addAttribute("transferBalance",balance);
        model.addAttribute("transferMax",balance.max(BigDecimal.ZERO).min(new BigDecimal("999999999999.99")));
        model.addAttribute("transferRecipients",repo.transferRecipients(clinic(session),kassaId));
        model.addAttribute("transferAccounts",repo.accountingCodes(clinic(session),"KASSA_TRANSFER"));
        model.addAttribute("transferToken",issueToken(session,kassaId,null,"KASSA_TRANSFER"));
        return "pages/maliyye/kassa";
    }
    @PostMapping("/kassa/transfer")
    public String transfer(@RequestParam Long kassaId,@RequestParam String paymentToken,
            @RequestParam(required=false) Long recipientId,@RequestParam(required=false) Long accountId,
            @RequestParam(required=false) BigDecimal amount,@RequestParam(required=false) String note,
            Authentication auth,HttpSession session,RedirectAttributes flash) {
        service.requireCash(auth,clinic(session),kassaId,true);
        boolean success=false;
        try {
            consumeToken(session,paymentToken,kassaId,null,"KASSA_TRANSFER");
            var result=service.transfer(auth,clinic(session),kassaId,recipientId,accountId,amount,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",msg(success?"kassa.transferSuccess":"kassa.transferFailed"));
            if(!success) log.warn("Cash transfer rejected: status={}",result.get("status_kodu"));
        } catch(PaymentValidationException e) {
            flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) {
            log.error("Cash transfer outcome requires verification",e);
            flash.addFlashAttribute("errorMessage",msg("kassa.paymentUnknown"));
        }
        flash.addAttribute("kassaId",kassaId);
        if(!success) {
            flash.addFlashAttribute("transferRecipientId",recipientId);flash.addFlashAttribute("transferAccountId",accountId);
            flash.addFlashAttribute("transferAmount",amount);flash.addFlashAttribute("transferNote",note);
        }
        return success?"redirect:/kassa":"redirect:/kassa/transfer";
    }
    @GetMapping("/kassa/qebzler")
    public String receiptsPage(@RequestParam Long kassaId,@RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="") String from,@RequestParam(defaultValue="") String to,
            @RequestParam(defaultValue="") String direction,@RequestParam(defaultValue="") String operation,
            @RequestParam(required=false) String active,@RequestParam(defaultValue="1") int page,
            @RequestParam(required=false) Long receiptId,@RequestParam(defaultValue="false") boolean cancel,
            Authentication auth,HttpSession session,Model model,jakarta.servlet.http.HttpServletRequest request) {
        if(active==null) active="true";
        String today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")).toString();
        if(from.isBlank()) from=today;
        if(to.isBlank()) to=today;
        model.addAttribute("cash",service.requireCash(auth,clinic(session),kassaId,false));
        model.addAttribute("canCancelReceipts",service.canCancelReceipt(auth,clinic(session)) && Boolean.TRUE.equals(((Map<?,?>)model.getAttribute("cash")).get("islesin")));
        model.addAttribute("registers",service.cashRegisters(auth,clinic(session)));
        model.addAttribute("cashId",kassaId);model.addAttribute("q",q);model.addAttribute("from",from);model.addAttribute("to",to);
        model.addAttribute("direction",direction);model.addAttribute("operation",operation);model.addAttribute("active",active);
        model.addAttribute("page",Math.max(1,page));
        List<Map<String,Object>> rows=List.of();
        try {
            var start=from.isBlank()?null:java.time.LocalDate.parse(from);
            var end=to.isBlank()?null:java.time.LocalDate.parse(to);
            if(page<1 || q.length()>200 || (!direction.isEmpty() && !List.of("GIRIS","CIXIS").contains(direction))
                    || operation.length()>64 || !List.of("","true","false").contains(active)
                    || (start!=null && end!=null && start.isAfter(end))) throw new IllegalArgumentException();
            rows=repo.allReceipts(clinic(session),kassaId,q.trim(),start,end,direction,operation,active.isEmpty()?null:Boolean.valueOf(active),page);
        } catch(java.time.DateTimeException | IllegalArgumentException e) {
            model.addAttribute("errorMessage",msg("kassa.receiptInvalidFilter"));
        } catch(DataAccessException e) {
            log.error("Receipt list lookup failed",e);model.addAttribute("errorMessage",msg("kassa.receiptLoadFailed"));
        }
        model.addAttribute("receipts",rows.stream().limit(100).toList());model.addAttribute("hasMore",rows.size()>100);
        if(receiptId!=null) {
            receiptPage(receiptId,kassaId,auth,session,model,request);
            model.addAttribute("showReceiptPopup",true);model.addAttribute("cancelMode",cancel);
        }
        base(model,"kassa.receipts");return "pages/maliyye/kassa-qebzler";
    }
    @GetMapping({"/kassa/qebzler/{receiptId}","/kassa/qebzler/{receiptId}/cap"})
    public String receiptPage(@PathVariable Long receiptId,@RequestParam Long kassaId,Authentication auth,
            HttpSession session,Model model,jakarta.servlet.http.HttpServletRequest request) {
        var cash=service.requireCash(auth,clinic(session),kassaId,false);
        boolean print=request.getRequestURI().endsWith("/cap");
        var detail=repo.receiptDetail(clinic(session),kassaId,receiptId);
        var paper=repo.receiptPrint(clinic(session),kassaId,receiptId);
        if(detail.isEmpty() || paper.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,msg("kassa.receiptNotFound"));
        model.addAttribute("cash",cash);model.addAttribute("cashId",kassaId);model.addAttribute("receiptId",receiptId);
        model.addAttribute("receipt",detail);model.addAttribute("paper",paper);
        var source=print?paper:detail;
        var payments=receiptJson(source.get("odenisler"));var services=receiptJson(source.get("xidmetler"));
        model.addAttribute("receiptPayments",payments);model.addAttribute("receiptServices",services);
        model.addAttribute("receiptPaymentTotal",payments.stream().map(r->KassaEmeliyyatService.money(r,"mebleg")).reduce(BigDecimal.ZERO,BigDecimal::add));
        model.addAttribute("receiptServiceTotal",services.stream().map(r->KassaEmeliyyatService.money(r,"mebleg")).reduce(BigDecimal.ZERO,BigDecimal::add));
        boolean cancelPermission=Boolean.TRUE.equals(cash.get("islesin")) && service.canCancelReceipt(auth,clinic(session));
        model.addAttribute("cancelPermission",cancelPermission);
        boolean canCancel=cancelPermission && "TAMAMLANIB".equals(paper.get("status"));
        model.addAttribute("canCancel",canCancel);
        model.addAttribute("cancelReasons",!print?service.receiptCancelReasons():List.of());
        model.addAttribute("cancelToken",canCancel && !print?issueToken(session,kassaId,receiptId,"QEBZ_LEGV"):"");
        base(model,"kassa.receiptDetail");return print?"pages/maliyye/kassa-qebz-cap":"pages/maliyye/kassa-qebz-detail";
    }
    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> receiptJson(Object value) {
        if(value==null) return List.of();
        return tools.jackson.databind.json.JsonMapper.builder().enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build().readValue(value.toString(),List.class);
    }
    @PostMapping("/kassa/qebzler/{receiptId}/legv")
    public String cancelReceipt(@PathVariable Long receiptId,@RequestParam Long kassaId,@RequestParam String paymentToken,
            @RequestParam(required=false) Long reasonId,Authentication auth,HttpSession session,RedirectAttributes flash) {
        service.requireCash(auth,clinic(session),kassaId,true);
        if(!service.canCancelReceipt(auth,clinic(session))) throw new AccessDeniedException("kassa.denied");
        try {
            consumeToken(session,paymentToken,kassaId,receiptId,"QEBZ_LEGV");
            var result=service.cancelReceipt(auth,clinic(session),kassaId,receiptId,reasonId);
            boolean success=Boolean.TRUE.equals(result.get("ugurlu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",msg(success?"kassa.receiptCancelSuccess":"kassa.receiptCancelFailed"));
            if(!success) log.warn("Receipt cancellation rejected: status={}",result.get("status_kodu"));
        } catch(PaymentValidationException e) { flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) { log.error("Receipt cancellation outcome requires verification",e);flash.addFlashAttribute("errorMessage",msg("kassa.paymentUnknown")); }
        flash.addAttribute("kassaId",kassaId);
        flash.addAttribute("receiptId",receiptId);
        return "redirect:/kassa/qebzler";
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
    @GetMapping({"/kassa/diger-giris","/kassa/avans","/kassa/diger-cixis"})
    public String otherIncome(@RequestParam Long kassaId, Authentication auth, HttpSession session, Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        boolean advance=request.getRequestURI().endsWith("/avans");
        boolean expense=request.getRequestURI().endsWith("/diger-cixis");
        var cash=service.requireCash(auth,clinic(session),kassaId,true);
        home(kassaId,auth,session,model);
        model.addAttribute("incomeCash",cash);
        model.addAttribute("incomeAccounts",repo.accountingCodes(clinic(session),advance?"AVANS_QEBUL":expense?"DIGER_CIXIS":"DIGER_GIRIS"));
        model.addAttribute("incomePaymentTypes",repo.paymentTypes());
        model.addAttribute("incomeToken",issueToken(session,kassaId,null,advance?"AVANS_QEBUL":expense?"DIGER_CIXIS":"DIGER_GIRIS"));
        model.addAttribute("showOtherIncome",true);
        model.addAttribute("advanceMode",advance);
        model.addAttribute("expenseMode",expense);
        if(advance) {
            model.addAttribute("advanceCardTypes",repo.cardTypes());
            Long selectedVisit=(Long)model.getAttribute("advanceVisitId");
            var selected=selectedVisit==null?Map.<String,Object>of():repo.advanceVisit(clinic(session),kassaId,selectedVisit);
            model.addAttribute("advanceSelected",selected);
        }
        return "pages/maliyye/kassa";
    }
    @PostMapping({"/kassa/diger-giris","/kassa/avans","/kassa/diger-cixis"})
    public String saveOtherIncome(@RequestParam Long kassaId, @RequestParam String paymentToken,
            @RequestParam(required=false) Long accountId, @RequestParam(required=false) List<Long> paymentTypeIds,
            @RequestParam(required=false) List<BigDecimal> amounts, @RequestParam(required=false) String note,
            @RequestParam(required=false) Long visitId, jakarta.servlet.http.HttpServletRequest request,
            Authentication auth, HttpSession session, RedirectAttributes flash) {
        boolean advance=request.getRequestURI().endsWith("/avans");
        boolean expense=request.getRequestURI().endsWith("/diger-cixis");
        service.requireCash(auth,clinic(session),kassaId,true);
        boolean success=false;
        try {
            consumeToken(session,paymentToken,kassaId,null,advance?"AVANS_QEBUL":expense?"DIGER_CIXIS":"DIGER_GIRIS");
            var result=advance?service.advance(auth,clinic(session),kassaId,visitId,accountId,paymentTypeIds,amounts,note)
                    :expense?service.otherExpense(auth,clinic(session),kassaId,accountId,paymentTypeIds,amounts,note)
                    :service.otherIncome(auth,clinic(session),kassaId,accountId,paymentTypeIds,amounts,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            String code=String.valueOf(result.get("status_kodu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",messages.getMessage(
                    success?(advance?"kassa.advanceSuccess":expense?"kassa.expenseSuccess":"kassa.incomeSuccess"):"kassa.result."+code,null,msg(expense?"kassa.expenseFailed":"kassa.incomeFailed"),LocaleContextHolder.getLocale()));
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
        return success?"redirect:/kassa":advance?"redirect:/kassa/avans":expense?"redirect:/kassa/diger-cixis":"redirect:/kassa/diger-giris";
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
                    :repo.advancePatients(clinic(session),kassaId,card,query,from,to,page);
            model.addAttribute("advanceResults",rows.stream().limit(100).toList());
            model.addAttribute("advanceHasMore",rows.size()>100);
        }
        model.addAttribute("advancePage",Math.max(1,page));
        return "fragments/kassa-advance-search :: results";
    }
    @GetMapping("/kassa/avans-qaytarma")
    public String advanceRefundPage(@RequestParam Long kassaId,@RequestParam(required=false) Long targetId,
            @RequestParam(defaultValue="") String q,@RequestParam(required=false) String cardType,
            @RequestParam(defaultValue="1") int page,Authentication auth,HttpSession session,Model model) {
        var cash=service.requireCash(auth,clinic(session),kassaId,false);
        var cards=repo.cardTypes();String card=cardType==null || cardType.isBlank()?null:cardType.trim();
        boolean invalid=page<1 || q.length()>200 || (card!=null && cards.stream().noneMatch(r->card.equals(r.get("kod"))));
        var patients=invalid?List.<Map<String,Object>>of():repo.advanceRefundPatients(clinic(session),kassaId,card,q.trim(),null,null,page);
        var advances=targetId==null?List.<Map<String,Object>>of():repo.refundableAdvances(clinic(session),kassaId,targetId);
        if(invalid) model.addAttribute("errorMessage",msg("kassa.invalidAdvanceSearch"));
        if(targetId!=null && advances.isEmpty()) model.addAttribute("errorMessage",msg("kassa.advanceRefundStale"));
        model.addAttribute("cash",cash);model.addAttribute("cashId",kassaId);model.addAttribute("targetId",targetId);
        model.addAttribute("patients",patients.stream().limit(100).toList());model.addAttribute("hasMore",patients.size()>100);
        model.addAttribute("cardTypes",cards);model.addAttribute("cardType",card);model.addAttribute("query",q);model.addAttribute("page",Math.max(1,page));
        model.addAttribute("advances",advances);model.addAttribute("selected",advances.isEmpty()?null:advances.getFirst());
        model.addAttribute("canWrite",Boolean.TRUE.equals(cash.get("islesin")));
        model.addAttribute("refundAccounts",advances.isEmpty()?List.of():repo.accountingCodes(clinic(session),"AVANS_QAYTARMA"));
        model.addAttribute("paymentTypes",advances.isEmpty()?List.of():repo.paymentTypes());
        model.addAttribute("paymentToken",advances.isEmpty()?"":issueToken(session,kassaId,targetId,"AVANS_QAYTARMA"));
        base(model,"kassa.advanceRefund");return "pages/maliyye/kassa-avans-qaytarma";
    }
    @PostMapping("/kassa/avans-qaytarma")
    public String refundAdvance(@RequestParam Long kassaId,@RequestParam Long targetId,@RequestParam String paymentToken,
            @RequestParam(required=false) Long advanceId,@RequestParam(required=false) Long accountId,@RequestParam(required=false) Long typeId,
            @RequestParam(required=false) BigDecimal amount,@RequestParam(required=false) String note,Authentication auth,HttpSession session,RedirectAttributes flash) {
        service.requireCash(auth,clinic(session),kassaId,true);boolean success=false;
        try {
            consumeToken(session,paymentToken,kassaId,targetId,"AVANS_QAYTARMA");
            var result=service.refundAdvance(auth,clinic(session),kassaId,targetId,advanceId,accountId,typeId,amount,note);
            success=Boolean.TRUE.equals(result.get("ugurlu"));
            flash.addFlashAttribute(success?"successMessage":"errorMessage",msg(success?"kassa.advanceRefundSuccess":"kassa.advanceRefundFailed"));
            if(!success) log.warn("Advance refund rejected: status={}",result.get("status_kodu"));
        } catch(PaymentValidationException e) {flash.addFlashAttribute("errorMessage",msg(e.getMessage()));
        } catch(DataAccessException e) {log.error("Advance refund outcome requires verification",e);flash.addFlashAttribute("errorMessage",msg("kassa.paymentUnknown"));}
        flash.addAttribute("kassaId",kassaId);
        if(!success) {
            flash.addAttribute("targetId",targetId);flash.addFlashAttribute("refundAdvanceId",advanceId);flash.addFlashAttribute("refundAccountId",accountId);
            flash.addFlashAttribute("refundTypeId",typeId);flash.addFlashAttribute("refundAmount",amount);flash.addFlashAttribute("refundNote",note);
        }
        return "redirect:/kassa/avans-qaytarma";
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
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ModelAndView receiptMissing(org.springframework.web.server.ResponseStatusException e) {
        return new ModelAndView("pages/maliyye/kassa-receipt-error",Map.of("errorMessage",msg("kassa.receiptNotFound")),e.getStatusCode());
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
