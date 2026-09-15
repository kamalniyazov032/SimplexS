package az.simplexs.simplexs.service.eczaxana;

import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.repository.eczaxana.QaimeRepository.Operation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class QaimeService {
    private final QaimeRepository repo;
    private final AnbarKontekstRepository context;
    private final ObjectMapper json;
    public QaimeService(QaimeRepository repo,AnbarKontekstRepository context,ObjectMapper json){this.repo=repo;this.context=context;this.json=json;}
    public record Scope(Long clinic,Long personal,Long warehouse,int year) {}
    public static class Rejected extends RuntimeException {
        private static final long serialVersionUID=1L;
        public final int status;
        public Rejected(int status,String code){super(code);this.status=status;}
    }
    public void authorize(Long clinic,Long personal){if(!context.canAccess(clinic,personal))throw new Rejected(403,"denied");}
    public void warehouse(Long clinic,Long personal,Long warehouse){
        authorize(clinic,personal);
        if(context.warehouses(clinic,personal).stream().noneMatch(w->Objects.equals(w.anbarId(),warehouse)))throw new Rejected(403,"denied");
    }
    public void validate(Scope s){
        warehouse(s.clinic(),s.personal(),s.warehouse());
        if(context.years(s.clinic(),s.warehouse()).stream().noneMatch(y->Objects.equals(y.il(),s.year())))throw new Rejected(400,"invalidYear");
    }
    public Map<String,Object> invoice(Scope s,Long id){return repo.invoice(s.clinic(),s.warehouse(),s.year(),id).orElseThrow(()->new Rejected(404,"notFound"));}
    public Map<String,Object> details(Scope s,Long id){
        validate(s);var header=invoice(s,id);var rows=repo.materials(s.clinic(),s.warehouse(),id);
        return Map.of("invoice",header,"rows",rows,"purchaseTotal",sum(rows,"alis_meblegi"),"saleTotal",sum(rows,"satis_meblegi"));
    }
    private BigDecimal sum(List<Map<String,Object>> rows,String key){return rows.stream().map(r->r.get(key) instanceof BigDecimal d?d:BigDecimal.ZERO).reduce(BigDecimal.ZERO,(a,b)->a.add(b));}
    @Transactional
    public Map<String,Object> write(Scope s,Operation op,Long invoiceId,Long materialId,Map<String,Object> input){
        validate(s);
        var values=new HashMap<>(input);
        if(op==Operation.CREATE||op==Operation.PREPARE){
            try{if(LocalDate.parse(String.valueOf(input.get("qaime_tarixi"))).getYear()!=s.year())throw new Rejected(400,"invalidYear");}
            catch(java.time.format.DateTimeParseException e){throw new Rejected(400,"invalidDate");}
        } else {
            invoice(s,invoiceId);
            if(op==Operation.UPDATE_MATERIAL||op==Operation.DELETE_MATERIAL){
                var item=repo.materials(s.clinic(),s.warehouse(),invoiceId).stream()
                    .filter(m->m.get("qaime_material_id") instanceof Number n && n.longValue()==materialId)
                    .findFirst().orElseThrow(()->new Rejected(404,"notFound"));
                if(!Boolean.TRUE.equals(item.get(op==Operation.DELETE_MATERIAL?"siline_biler":"deyisdirile_biler")))throw new Rejected(409,"materialLocked");
            }
        }
        if(op==Operation.CREATE){
            if(!(input.get("materiallar") instanceof List<?> materials)||materials.isEmpty()||materials.size()>1000)throw new Rejected(400,"materialsRequired");
            values.put("materiallar",json.writeValueAsString(materials));
        }
        values.put("klinika_id",s.clinic());values.put("anbar_id",s.warehouse());
        values.put("yaradan_personal_id",s.personal());values.put("yenileyen_personal_id",s.personal());
        values.put("qaime_id",invoiceId);values.put("qaime_material_id",materialId);
        var result=repo.execute(op,values);
        if(!"UGURLU".equals(result.get("status_kodu")))throw new Rejected(422,String.valueOf(result.get("status_kodu")));
        return result;
    }
}
