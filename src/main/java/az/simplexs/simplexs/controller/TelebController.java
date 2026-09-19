package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/eczaxana/telebler")
public class TelebController {
    private final TelebRepository repo;
    private final AnbarKontekstRepository context;
    private final AccessService access;
    private final MessageSource messages;
    private final ObjectMapper json;
    public TelebController(TelebRepository repo, AnbarKontekstRepository context, AccessService access, MessageSource messages, ObjectMapper json) {
        this.repo=repo;this.context=context;this.access=access;this.messages=messages;this.json=json;
    }
    private record Scope(Long clinic, Long personal, Long warehouse) {}
    private static class Rejected extends RuntimeException {
        final int status;
        Map<String,Object> result;
        Rejected(int status, String key) { super(key);this.status=status; }
    }
    private Scope scope(Authentication auth, HttpSession session, Long warehouse, String side) {
        Long clinic=(Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        if(auth==null || !(auth.getPrincipal() instanceof AuthenticatedPersonal personal) || clinic==null || !access.hasClinic(auth,clinic))throw new Rejected(403,"denied");
        if(!Set.of("GONDERILEN","GELEN").contains(side))throw new Rejected(400,"invalidInput");
        String code="HIS_PHARMACY_REQUESTS_SENT";
        if(!repo.modules(clinic,personal.personalId()).contains(code))throw new Rejected(403,"denied");
        if(warehouse!=null && context.warehouses(clinic,personal.personalId()).stream().noneMatch(w->Objects.equals(w.anbarId(),warehouse)))throw new Rejected(403,"denied");
        return new Scope(clinic,personal.personalId(),warehouse);
    }
    private Map<String,Object> request(Scope s, Long id, String side) {
        return repo.list(s.clinic(),s.warehouse(),side,null,null,null).stream()
                .filter(r->r.get("teleb_id") instanceof Number n && n.longValue()==id)
                .findFirst().orElseThrow(()->new Rejected(404,"notFound"));
    }
    private void counterpart(Scope s, Long target) {
        if(repo.counterpart(s.warehouse()).stream().noneMatch(r->r.get("anbar_id") instanceof Number n && n.longValue()==target))throw new Rejected(403,"denied");
    }
    private Map<String,Object> checked(Map<String,Object> result) {
        if(!"UGURLU".equals(result.get("status_kodu"))){var e=new Rejected(422,"saveFailed");e.result=result;throw e;}
        return result;
    }

    @GetMapping({"","/qarsilama","/yeni","/{id}/redakte"})
    public String page(@PathVariable(required=false) Long id, @RequestParam(required=false) Long anbarId,
                       HttpServletRequest request, Authentication auth, HttpSession session, Model model) {
        String side=request.getServletPath().endsWith("/qarsilama")?"GELEN":"GONDERILEN";
        var s=scope(auth,session,anbarId,side);
        var modules=repo.modules(s.clinic(),s.personal());
        model.addAttribute("pageTitle",messages.getMessage("teleb.title",null,LocaleContextHolder.getLocale()));
        model.addAttribute("warehouses",context.warehouses(s.clinic(),s.personal()));
        model.addAttribute("canSent",modules.contains("HIS_PHARMACY_REQUESTS_SENT"));
        model.addAttribute("canIncoming",modules.contains("HIS_PHARMACY_REQUESTS_SENT"));
        model.addAttribute("side",side);
        model.addAttribute("editor",id!=null || request.getServletPath().endsWith("/yeni"));
        model.addAttribute("editId",id);
        model.addAttribute("warehouseId",anbarId);
        return "pages/eczaxana/telebler";
    }

    @GetMapping("/iller") @ResponseBody
    public Object years(@RequestParam Long anbarId, @RequestParam String teref, Authentication auth, HttpSession session) {
        var s=scope(auth,session,anbarId,teref);return context.years(s.clinic(),anbarId);
    }
    @GetMapping("/qarsi-anbarlar") @ResponseBody
    public Object counterparts(@RequestParam Long anbarId, Authentication auth, HttpSession session) {
        scope(auth,session,anbarId,"GONDERILEN");return repo.counterpart(anbarId);
    }
    @GetMapping("/qruplar") @ResponseBody
    public Object groups(@RequestParam Long anbarId,@RequestParam Long qarsiAnbarId,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GONDERILEN");counterpart(s,qarsiAnbarId);return repo.groups(qarsiAnbarId);
    }
    @GetMapping("/materiallar") @ResponseBody
    public Object catalog(@RequestParam Long anbarId,@RequestParam Long qarsiAnbarId,@RequestParam(required=false) Long qrupId,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GONDERILEN");counterpart(s,qarsiAnbarId);return repo.catalog(qarsiAnbarId,qrupId);
    }
    @GetMapping("/vahidler") @ResponseBody
    public Object units(@RequestParam Long anbarId,@RequestParam Long qarsiAnbarId,@RequestParam Long materialId,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GONDERILEN");counterpart(s,qarsiAnbarId);
        if(repo.catalog(qarsiAnbarId,null).stream().noneMatch(m->m.get("material_id") instanceof Number n && n.longValue()==materialId))throw new Rejected(404,"notFound");
        return repo.units(materialId);
    }
    @GetMapping("/siyahi") @ResponseBody
    public Object list(@RequestParam Long anbarId,@RequestParam String teref,@RequestParam(required=false) Integer il,
                       @RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String from,@RequestParam(defaultValue="") String to,
                       @RequestParam(defaultValue="") String nomre,@RequestParam(required=false) Long qarsiAnbarId,
                       @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="25") int size,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,teref);
        if(page<1 || page>100000 || !Set.of(10,25,50,100).contains(size))throw new Rejected(400,"invalidInput");
        try {
            LocalDate start=from.isBlank()?null:LocalDate.parse(from),end=to.isBlank()?null:LocalDate.parse(to);
            if(il!=null){
                if(il<1 || il>9999)throw new Rejected(400,"invalidInput");
                if(start==null || start.isBefore(LocalDate.of(il,1,1)))start=LocalDate.of(il,1,1);
                if(end==null || end.isAfter(LocalDate.of(il,12,31)))end=LocalDate.of(il,12,31);
            }
            if(start!=null && end!=null && start.isAfter(end))throw new Rejected(400,"invalidDate");
            from=start==null?"":start.toString();to=end==null?"":end.toString();
        }catch(java.time.format.DateTimeParseException e){throw new Rejected(400,"invalidDate");}
        var rows=repo.list(s.clinic(),anbarId,teref,status.isBlank()?null:status,from.isBlank()?null:from,to.isBlank()?null:to);
        rows=rows.stream()
                .filter(row->nomre.isBlank() || ("T-"+String.format("%05d",((Number)row.get("teleb_id")).longValue())).toLowerCase(java.util.Locale.ROOT).contains(nomre.trim().toLowerCase(java.util.Locale.ROOT)))
                .filter(row->qarsiAnbarId==null || row.get("qarsilayan_anbar_id") instanceof Number n && n.longValue()==qarsiAnbarId)
                .toList();
        var pageRows=rows.stream().skip((long)(page-1)*size).limit(size).toList();
        if(teref.equals("GONDERILEN"))for(var row:pageRows)row.put("redakte_edile_biler",repo.editStatus(((Number)row.get("teleb_id")).longValue(),s.personal()).get("redakte_edile_biler"));
        return Map.of("rows",pageRows,"total",rows.size());
    }
    @GetMapping("/{id}/detal") @ResponseBody
    public Object detail(@PathVariable Long id,@RequestParam Long anbarId,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GONDERILEN");var header=request(s,id,"GONDERILEN");
        return Map.of("request",header,"rows",repo.materials(id),"edit",repo.editStatus(id,s.personal()));
    }
    @PostMapping("/{id}/baxilir") @ResponseBody @Transactional
    public Object view(@PathVariable Long id,@RequestParam Long anbarId,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GELEN");request(s,id,"GELEN");return checked(repo.viewed(id,anbarId,s.personal()));
    }
    @GetMapping("/{id}/qarsilama-materiallari") @ResponseBody
    public Object fulfillmentMaterials(@PathVariable Long id,@RequestParam Long anbarId,@RequestParam String tarix,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GELEN");var header=request(s,id,"GELEN");
        return Map.of("request",header,"rows",repo.fulfillmentMaterials(id,tarix));
    }
    @PostMapping({"/yeni","/{id}/yenile"}) @ResponseBody @Transactional
    public Object save(@PathVariable(required=false) Long id,@RequestParam Long anbarId,@RequestBody Map<String,Object> data,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GONDERILEN");
        if(id!=null){request(s,id,"GONDERILEN");if(!Boolean.TRUE.equals(repo.editStatus(id,s.personal()).get("redakte_edile_biler")))throw new Rejected(409,"editLocked");}
        Long target;
        try{target=Long.valueOf(String.valueOf(data.get("qarsilayan_anbar_id")));}catch(NumberFormatException e){throw new Rejected(400,"invalidInput");}
        counterpart(s,target);
        if(!(data.get("materiallar") instanceof List<?> items)||items.isEmpty())throw new Rejected(400,"materialsRequired");
        return checked(repo.save(id,anbarId,target,Objects.toString(data.get("teleb_tarixi"),null),json.writeValueAsString(items),Objects.toString(data.get("aciqlama"),null),s.personal()));
    }
    @PostMapping("/{id}/qarsila") @ResponseBody @Transactional
    public Object fulfill(@PathVariable Long id,@RequestParam Long anbarId,@RequestBody Map<String,Object> data,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,"GELEN");request(s,id,"GELEN");
        if(!(data.get("qerarlar") instanceof List<?> items)||items.isEmpty())throw new Rejected(400,"decisionsRequired");
        return checked(repo.fulfill(id,anbarId,Objects.toString(data.get("tarix"),null),Objects.toString(data.get("saat"),null),json.writeValueAsString(items),Objects.toString(data.get("aciqlama"),null),s.personal()));
    }
    @GetMapping("/{id}/tarixce") @ResponseBody
    public Object history(@PathVariable Long id,@RequestParam Long anbarId,@RequestParam String teref,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,teref);request(s,id,teref);return repo.history(id);
    }
    @GetMapping("/{id}/tarixce/{historyId}") @ResponseBody
    public Object historyDetail(@PathVariable Long id,@PathVariable Long historyId,@RequestParam Long anbarId,@RequestParam String teref,Authentication auth,HttpSession session) {
        var s=scope(auth,session,anbarId,teref);request(s,id,teref);
        if(repo.history(id).stream().noneMatch(h->h.get("qarsilama_id") instanceof Number n && n.longValue()==historyId))throw new Rejected(404,"notFound");
        return repo.historyDetail(s.clinic(),historyId);
    }
    @ExceptionHandler(Rejected.class)
    public ResponseEntity<Map<String,Object>> rejected(Rejected e) {
        return ResponseEntity.status(e.status).body(e.result!=null?e.result:Map.of("message",messages.getMessage("teleb."+e.getMessage(),null,LocaleContextHolder.getLocale())));
    }
    @ExceptionHandler({DataAccessException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String,String>> failure(Exception e) {
        return ResponseEntity.status(e instanceof DataAccessException?503:400).body(Map.of("message",messages.getMessage("teleb."+(e instanceof DataAccessException?"loadFailed":"invalidInput"),null,LocaleContextHolder.getLocale())));
    }
}
