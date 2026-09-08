package az.simplexs.simplexs.service.kassa;

import az.simplexs.simplexs.repository.kassa.KassaEmeliyyatRepository;
import az.simplexs.simplexs.security.AccessService;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KassaEmeliyyatService {
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");
    private final KassaEmeliyyatRepository repo;
    private final AccessService access;
    public KassaEmeliyyatService(KassaEmeliyyatRepository repo, AccessService access) { this.repo=repo; this.access=access; }
    public List<Map<String,Object>> cashRegisters(Authentication auth, Long clinic) {
        if (!(auth != null && auth.getPrincipal() instanceof AuthenticatedPersonal)
                || !access.hasClinic(auth,clinic) || !access.canAccessRoute(auth,clinic,"/kassa"))
            throw new AccessDeniedException("kassa.denied");
        return repo.cashRegisters(clinic,personalId(auth)).stream()
                .filter(r -> Boolean.TRUE.equals(r.get("izlesin")) || Boolean.TRUE.equals(r.get("islesin"))).toList();
    }
    public Map<String,Object> requireCash(Authentication auth, Long clinic, Long cash, boolean write) {
        return cashRegisters(auth,clinic).stream().filter(r -> Objects.equals(id(r,"kassa_id"),cash))
                .filter(r -> !write || Boolean.TRUE.equals(r.get("islesin")))
                .findFirst().orElseThrow(() -> new AccessDeniedException("kassa.denied"));
    }
    public static Long personalId(Authentication auth) { return ((AuthenticatedPersonal)auth.getPrincipal()).personalId(); }
    public static Long id(Map<String,Object> row, String key) { return row.get(key) instanceof Number n ? n.longValue() : null; }
    public static BigDecimal money(Map<String,Object> row, String key) {
        return row.get(key) instanceof Number n ? new BigDecimal(n.toString()) : BigDecimal.ZERO;
    }
    public static boolean selectable(Map<String,Object> row, boolean debt) {
        return money(row,debt ? "qalan_borc" : "qalan_mebleg").signum()>0
                && (debt || (!Boolean.TRUE.equals(row.get("borclandirilib")) && !Boolean.TRUE.equals(row.get("paket_daxildir"))));
    }
    public boolean canBorrow(Authentication auth, Long clinic) { return access.hasPermission(auth,clinic,"SLX000004"); }
    @Transactional
    public Map<String,Object> pay(Authentication auth, Long clinic, Long cash, Long target, boolean debt,
            List<Long> selected, List<Long> types, List<BigDecimal> amounts, boolean borrow, String note) {
        requireCash(auth,clinic,cash,true);
        if (target==null || target<=0 || selected==null || selected.isEmpty() || selected.size()>1000
                || new HashSet<>(selected).size()!=selected.size() || selected.stream().anyMatch(x->x==null || x<=0)) fail("selectServices");
        if (types==null || amounts==null || types.size()!=amounts.size() || types.isEmpty() || types.size()>20) fail("invalidPayment");
        if (note!=null && note.length()>1000) fail("noteLong");
        if (borrow && (debt || !canBorrow(auth,clinic))) throw new AccessDeniedException("kassa.denied");
        if (!repo.lockPatient(clinic,target,debt)) fail("staleServices");
        var rows=debt ? repo.debtServices(clinic,cash,target) : repo.services(clinic,cash,target);
        String key=debt ? "borclandirma_xidmet_id" : "xeste_xidmet_id";
        Map<Long,BigDecimal> available=new HashMap<>();
        rows.stream().filter(r->selectable(r,debt)).forEach(r->available.put(id(r,key),money(r,debt?"qalan_borc":"qalan_mebleg")));
        if (!available.keySet().containsAll(selected)) fail("staleServices");
        var payments = validatePayments(types,amounts);
        BigDecimal total = payments.total();
        BigDecimal due=selected.stream().map(available::get).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.signum()<=0 || total.compareTo(MAX_MONEY)>0) fail("invalidPayment");
        if(total.compareTo(due)>0) fail("overpayment");
        if(!debt && total.compareTo(due)<0 && !borrow) fail("borrowRequired");
        String serviceJson=selected.stream().map(x->"{\""+key+"\":"+x+"}").collect(Collectors.joining(",","[","]"));
        return repo.pay(clinic,cash,personalId(auth),target,debt,serviceJson,
                payments.json(),borrow,note==null?null:note.trim());
    }
    @Transactional
    public Map<String,Object> otherIncome(Authentication auth, Long clinic, Long cash, Long account,
            List<Long> types, List<BigDecimal> amounts, String note) {
        requireCash(auth,clinic,cash,true);
        if (note!=null && note.length()>1000) fail("noteLong");
        if (account==null || repo.accountingCodes(clinic,"DIGER_GIRIS").stream()
                .noneMatch(row -> Objects.equals(id(row,"muhasibat_kodu_id"),account))) fail("invalidAccount");
        var payments=validatePayments(types,amounts);
        return repo.otherIncome(clinic,cash,personalId(auth),account,payments.json(),note==null?null:note.trim());
    }
    @Transactional
    public Map<String,Object> advance(Authentication auth,Long clinic,Long cash,Long visit,Long account,
            List<Long> types,List<BigDecimal> amounts,String note) {
        requireCash(auth,clinic,cash,true);
        if(visit==null || visit<=0 || !repo.lockPatient(clinic,visit,false)) fail("selectAdvancePatient");
        var patient=repo.advanceVisit(clinic,visit);
        if(id(patient,"xeste_id")==null) fail("selectAdvancePatient");
        if(account==null || repo.accountingCodes(clinic,"AVANS_QEBUL").stream()
                .noneMatch(row->Objects.equals(id(row,"muhasibat_kodu_id"),account))) fail("invalidAccount");
        if(note!=null && note.length()>1000) fail("noteLong");
        var payments=validatePayments(types,amounts);
        return repo.advance(clinic,cash,personalId(auth),id(patient,"xeste_id"),visit,account,payments.json(),note==null?null:note.trim());
    }
    @Transactional
    public Map<String,Object> refund(Authentication auth,Long clinic,Long cash,Long visit,List<Long> selected,
            Long account,Long paymentType,BigDecimal expectedTotal,String note) {
        requireCash(auth,clinic,cash,true);
        if(visit==null || visit<=0 || selected==null || selected.isEmpty() || selected.size()>1000
                || selected.stream().anyMatch(id->id==null || id<=0) || new HashSet<>(selected).size()!=selected.size()) fail("selectRefundServices");
        if(note!=null && note.length()>1000) fail("noteLong");
        if(!repo.lockPatient(clinic,visit,false)) fail("refundStale");
        var rows=repo.refundServices(clinic,cash,visit);
        var eligible=rows.stream().filter(r->Objects.equals(id(r,"gelis_id"),visit)
                && "YENI".equals(r.get("status_kodu")) && money(r,"qaytarila_bilen_mebleg").signum()>0).toList();
        Map<Long,Map<String,Object>> byId=new HashMap<>();
        eligible.forEach(r->byId.put(id(r,"xeste_xidmet_id"),r));
        if(!byId.keySet().containsAll(selected)) fail("refundStale");
        String protocol=(String)byId.get(selected.getFirst()).get("protokol_kodu");
        if(protocol==null || protocol.isBlank() || selected.stream().anyMatch(id->!protocol.equals(byId.get(id).get("protokol_kodu")))) fail("refundStale");
        BigDecimal total=selected.stream().map(id->money(byId.get(id),"qaytarila_bilen_mebleg")).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(expectedTotal==null || total.compareTo(expectedTotal)!=0 || total.compareTo(MAX_MONEY)>0) fail("refundAmountChanged");
        if(account==null || repo.accountingCodes(clinic,"XESTE_QAYTARMA").stream().noneMatch(r->Objects.equals(id(r,"muhasibat_kodu_id"),account))) fail("invalidAccount");
        if(paymentType==null || repo.paymentTypes().stream().noneMatch(r->Objects.equals(id(r,"odenis_novu_id"),paymentType))) fail("invalidPayment");
        String json=selected.stream().map(id->"{\"xeste_xidmet_id\":"+id+"}").collect(Collectors.joining(",","[","]"));
        return repo.refund(clinic,cash,personalId(auth),protocol,account,json,paymentType,note==null?null:note.trim());
    }
    private record PaymentAmounts(BigDecimal total, String json) {}
    private PaymentAmounts validatePayments(List<Long> types,List<BigDecimal> amounts) {
        if (types==null || amounts==null || types.size()!=amounts.size() || types.isEmpty() || types.size()>20) fail("invalidPayment");
        Set<Long> allowed=repo.paymentTypes().stream().map(r->id(r,"odenis_novu_id")).collect(Collectors.toSet());
        BigDecimal total=BigDecimal.ZERO;
        List<String> paymentJson=new ArrayList<>();
        Set<Long> seen=new HashSet<>();
        for(int i=0;i<types.size();i++) {
            BigDecimal amount=amounts.get(i);
            // Empty amounts let the user leave unused payment methods untouched.
            if(amount==null || amount.signum()==0) continue;
            if(!allowed.contains(types.get(i)) || !seen.add(types.get(i)) || amount.signum()<0
                    || amount.stripTrailingZeros().scale()>2 || amount.compareTo(MAX_MONEY)>0) fail("invalidPayment");
            total=total.add(amount);
            paymentJson.add("{\"odenis_novu_id\":"+types.get(i)+",\"mebleg\":"+amount.toPlainString()+"}");
        }
        if(total.signum()<=0 || total.compareTo(MAX_MONEY)>0) fail("invalidPayment");
        return new PaymentAmounts(total,"["+String.join(",",paymentJson)+"]");
    }
    private static void fail(String key) { throw new PaymentValidationException(key); }
    public static class PaymentValidationException extends RuntimeException {
        public PaymentValidationException(String key) { super("kassa."+key); }
    }
}
